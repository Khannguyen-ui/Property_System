package com.homeverse.property.service.impl;

import com.homeverse.property.dto.NotificationEvent;
import com.homeverse.property.dto.request.AppointmentCreateRequest;
import com.homeverse.property.dto.response.AppointmentResponse;
import com.homeverse.property.entity.Appointment;
import com.homeverse.property.entity.Property;
import com.homeverse.property.kafka.producer.AppointmentNotificationProducer;
import com.homeverse.property.repository.AppointmentRepository;
import com.homeverse.property.repository.PropertyRepository;
import com.homeverse.property.service.AppointmentService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentNotificationProducer notificationProducer;
    private final PropertyRepository propertyRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getMyCalendar(Long currentUserId) {
        return appointmentRepository
                .findByUserIdOrOwnerIdOrderByAppointmentTimeAsc(currentUserId, currentUserId)
                .stream()
                .map(a -> toResponse(a, currentUserId))
                .toList();
    }

    @Override
    @Transactional
    public AppointmentResponse create(Long currentUserId, AppointmentCreateRequest request) {
        LocalDateTime time = request.getAppointmentTime() != null
                ? request.getAppointmentTime()
                : request.getScheduledAt();

        if (request.getPropertyId() == null) {
            throw new IllegalArgumentException("Thiếu propertyId");
        }

        if (time == null) {
            throw new IllegalArgumentException("Vui lòng chọn thời gian hẹn");
        }

        if (time.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Thời gian hẹn không được nằm trong quá khứ");
        }

        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bất động sản"));

        if (property.getStatus() != Property.Status.ACTIVE
                && property.getStatus() != Property.Status.APPROVED) {
            throw new IllegalArgumentException("Bất động sản chưa sẵn sàng để đặt lịch");
        }

        Long ownerId = property.getOwnerId();

        if (ownerId == null) {
            throw new IllegalArgumentException("Bất động sản chưa có chủ sở hữu");
        }

        if (Objects.equals(ownerId, currentUserId)) {
            throw new IllegalArgumentException("Bạn không thể tự đặt lịch với bài đăng của chính mình");
        }

        Appointment appointment = Appointment.builder()
                .propertyId(property.getId())
                .userId(currentUserId)
                .ownerId(ownerId)
                .appointmentTime(time)
                .note(request.getNote())
                .status(Appointment.Status.PENDING)
                .build();

        Appointment saved = appointmentRepository.save(appointment);

        notificationProducer.send(NotificationEvent.builder()
                .receiverId(saved.getOwnerId())
                .title("Có lịch hẹn mới")
                .content("Khách thuê đã gửi yêu cầu xem: " + property.getTitle())
                .type("APPOINTMENT_CREATED")
                .referenceId(saved.getId())
                .build());

        return toResponse(saved, currentUserId);
    }

    @Override
    @Transactional
    public AppointmentResponse updateStatus(Long currentUserId, Long id, Appointment.Status status) {
        Appointment appointment = getAppointment(id);

        if (!isParticipant(appointment, currentUserId)) {
            throw new IllegalArgumentException("Bạn không có quyền cập nhật lịch hẹn này");
        }

        if (appointment.getStatus() == Appointment.Status.COMPLETED
                || appointment.getStatus() == Appointment.Status.REJECTED
                || appointment.getStatus() == Appointment.Status.CANCELLED) {
            throw new IllegalArgumentException("Lịch hẹn đã kết thúc, không thể cập nhật");
        }

        if (status == Appointment.Status.ACCEPTED || status == Appointment.Status.REJECTED) {
            if (!Objects.equals(appointment.getOwnerId(), currentUserId)) {
                throw new IllegalArgumentException("Chỉ chủ bài đăng mới được duyệt hoặc từ chối lịch hẹn");
            }
        }

        if (status == Appointment.Status.CANCELLED) {
            if (!Objects.equals(appointment.getUserId(), currentUserId)
                    && !Objects.equals(appointment.getOwnerId(), currentUserId)) {
                throw new IllegalArgumentException("Bạn không có quyền hủy lịch hẹn này");
            }
        }

        if (status == Appointment.Status.SUGGESTED) {
            throw new IllegalArgumentException("Vui lòng dùng API đề xuất giờ mới");
        }

        appointment.setStatus(status);
        Appointment saved = appointmentRepository.save(appointment);

        Long receiverId = Objects.equals(currentUserId, saved.getOwnerId())
                ? saved.getUserId()
                : saved.getOwnerId();

        String title = switch (status) {
            case ACCEPTED -> "Lịch hẹn đã được chấp nhận";
            case REJECTED -> "Lịch hẹn đã bị từ chối";
            case CANCELLED -> "Lịch hẹn đã bị hủy";
            case COMPLETED -> "Lịch hẹn đã hoàn tất";
            default -> "Cập nhật lịch hẹn";
        };

        notificationProducer.send(NotificationEvent.builder()
                .receiverId(receiverId)
                .title(title)
                .content("Lịch hẹn bất động sản #" + saved.getPropertyId()
                        + " đã được cập nhật trạng thái: " + status.name())
                .type("APPOINTMENT_STATUS_UPDATED")
                .referenceId(saved.getId())
                .build());

        return toResponse(saved, currentUserId);
    }

    @Override
    @Transactional
    public AppointmentResponse suggestNewTime(Long currentUserId, Long id, LocalDateTime newTime, String note) {
        Appointment appointment = getAppointment(id);

        if (!Objects.equals(appointment.getOwnerId(), currentUserId)) {
            throw new IllegalArgumentException("Chỉ chủ bài đăng mới được đề xuất giờ mới");
        }

        if (newTime == null) {
            throw new IllegalArgumentException("Vui lòng chọn giờ mới");
        }

        if (newTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Giờ đề xuất không được nằm trong quá khứ");
        }

        appointment.setSuggestedTime(newTime);
        appointment.setSuggestedNote(note);
        appointment.setStatus(Appointment.Status.SUGGESTED);

        Appointment saved = appointmentRepository.save(appointment);

        notificationProducer.send(NotificationEvent.builder()
                .receiverId(saved.getUserId())
                .title("Chủ trọ đề xuất giờ mới")
                .content("Chủ trọ muốn đổi thời gian xem bất động sản #" + saved.getPropertyId())
                .type("APPOINTMENT_SUGGESTED")
                .referenceId(saved.getId())
                .build());

        return toResponse(saved, currentUserId);
    }

    @Override
    @Transactional
    public AppointmentResponse acceptSuggestion(Long currentUserId, Long id) {
        Appointment appointment = getAppointment(id);

        if (!Objects.equals(appointment.getUserId(), currentUserId)) {
            throw new IllegalArgumentException("Chỉ người đặt lịch mới được đồng ý giờ đề xuất");
        }

        if (appointment.getSuggestedTime() == null) {
            throw new IllegalArgumentException("Lịch hẹn này chưa có giờ đề xuất");
        }

        appointment.setAppointmentTime(appointment.getSuggestedTime());
        appointment.setSuggestedTime(null);
        appointment.setSuggestedNote(null);
        appointment.setStatus(Appointment.Status.ACCEPTED);

        Appointment saved = appointmentRepository.save(appointment);

        notificationProducer.send(NotificationEvent.builder()
                .receiverId(saved.getOwnerId())
                .title("Khách đã đồng ý giờ hẹn")
                .content("Khách thuê đã đồng ý thời gian xem bất động sản #" + saved.getPropertyId())
                .type("APPOINTMENT_SUGGESTION_ACCEPTED")
                .referenceId(saved.getId())
                .build());

        return toResponse(saved, currentUserId);
    }

    private Appointment getAppointment(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch hẹn"));
    }

    private boolean isParticipant(Appointment appointment, Long currentUserId) {
        return Objects.equals(appointment.getUserId(), currentUserId)
                || Objects.equals(appointment.getOwnerId(), currentUserId);
    }

    private AppointmentResponse toResponse(Appointment appointment, Long currentUserId) {
        boolean myRequest = Objects.equals(appointment.getUserId(), currentUserId);

        Long partnerId = myRequest
                ? appointment.getOwnerId()
                : appointment.getUserId();

        return AppointmentResponse.builder()
                .id(appointment.getId())
                .propertyId(appointment.getPropertyId())
                .userId(appointment.getUserId())
                .ownerId(appointment.getOwnerId())
                .partnerId(partnerId)
                .appointmentTime(appointment.getAppointmentTime())
                .scheduledAt(appointment.getAppointmentTime())
                .note(appointment.getNote())
                .status(appointment.getStatus().name())
                .suggestedTime(appointment.getSuggestedTime())
                .suggestedNote(appointment.getSuggestedNote())
                .myRequest(myRequest)
                .build();
    }
}