package com.homeverse.property.service.impl;

import com.homeverse.common.dto.RefundEvent;
import com.homeverse.common.exception.AppException;
import com.homeverse.common.exception.ErrorCode;
import com.homeverse.property.dto.response.PropertyResponseDTO;
import com.homeverse.property.entity.OutboxEvent;
import com.homeverse.property.entity.OwnerQuota;
import com.homeverse.property.entity.PromotionQueue;
import com.homeverse.property.entity.Property;
import com.homeverse.property.repository.OutboxRepository;
import com.homeverse.property.repository.OwnerQuotaRepository;
import com.homeverse.property.repository.PromotionQueueRepository;
import com.homeverse.property.repository.PropertyRepository;
import com.homeverse.property.service.AdminPropertyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPropertyServiceImpl implements AdminPropertyService {

    private final PropertyRepository propertyRepository;
    private final OwnerQuotaRepository ownerQuotaRepository;
    private final OutboxRepository outboxRepository;
    private final PromotionQueueRepository promotionQueueRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    // ==========================================
    // 1. XEM DANH SÁCH (CÓ BỘ LỌC)
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public Page<PropertyResponseDTO> getAllProperties(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size);

        // Nếu Admin truyền param status (VD: ?status=PENDING) -> Lọc ra để duyệt
        if (status != null && !status.isEmpty()) {
            try {
                Property.Status enumStatus = Property.Status.valueOf(status.toUpperCase());
                return propertyRepository.findByStatus(enumStatus, pageable).map(this::mapToResponse);
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
        }

        // Nếu không truyền status -> Lấy toàn bộ (Bỏ qua thùng rác nhờ @SQLRestriction
        // trên Entity)
        return propertyRepository.findAll(pageable).map(this::mapToResponse);
    }

    // ==========================================
    // 2. XEM CHI TIẾT
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public PropertyResponseDTO getPropertyDetail(Long id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROPERTY_NOT_FOUND));
        return mapToResponse(property);
    }

    @Override
@Transactional(rollbackFor = Exception.class)
public void updatePropertyStatus(Long adminId, Long id, String statusStr) {
    Property property = propertyRepository.findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.PROPERTY_NOT_FOUND));

    try {
        Property.Status newStatus = Property.Status.valueOf(statusStr.toUpperCase());
        Property.Status oldStatus = property.getStatus();

        if (oldStatus == Property.Status.PENDING && newStatus == Property.Status.ACTIVE) {
            if (!property.isQuotaDeducted()) {
                OwnerQuota quota = ownerQuotaRepository.findById(property.getOwnerId())
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

                if (quota.getFreePostsRemaining() <= 0) {
                    throw new AppException(ErrorCode.POST_LIMIT_EXCEEDED);
                }

                quota.setFreePostsRemaining(quota.getFreePostsRemaining() - 1);
                ownerQuotaRepository.save(quota);
                property.setQuotaDeducted(true);
            }

            promotionQueueRepository.findFirstByPropertyIdAndStatusOrderByPriorityLevelDescCreatedAtAsc(
                    id, PromotionQueue.PromotionStatus.WAITING).ifPresent(nextQueue -> {
                LocalDateTime now = LocalDateTime.now();
                nextQueue.setStatus(PromotionQueue.PromotionStatus.ACTIVE);
                nextQueue.setActivatedAt(now);
                nextQueue.setExpiresAt(now.plusDays(nextQueue.getDurationDays()));
                promotionQueueRepository.save(nextQueue);

                property.setIsPromoted(true);
                property.setPromotionPackageId(nextQueue.getPackageId());
                property.setPromotionPackageName(nextQueue.getPackageName());
                property.setPromotionExpiresAt(nextQueue.getExpiresAt());
            });
        } 
        else if (oldStatus == Property.Status.PENDING && newStatus == Property.Status.REJECTED) {
            
            List<PromotionQueue> waitingQueues = promotionQueueRepository.findByPropertyIdAndStatusOrderByPriorityLevelDescCreatedAtAsc(
                    id, PromotionQueue.PromotionStatus.WAITING);

            for (PromotionQueue queue : waitingQueues) {
                RefundEvent refundEvent = RefundEvent.builder()
                        .userId(property.getOwnerId())
                        .amount(queue.getAmount())
                        .transactionId(queue.getTransactionId())
                        .reason("Admin rejected property ID: " + id)
                        .build();

                outboxRepository.save(OutboxEvent.builder()
                        .topic("refund-payment-topic")
                        .payload(objectMapper.writeValueAsString(refundEvent))
                        .status("PENDING")
                        .createdAt(LocalDateTime.now())
                        .build());

                queue.setStatus(PromotionQueue.PromotionStatus.CANCELLED);
                promotionQueueRepository.save(queue);
            }

            if (property.isQuotaDeducted()) {
                property.setQuotaDeducted(false);
            }

            log.info("🚫 Admin rejected Property ID {}: Refund processed and promotions cancelled.", id);
        }

        property.setStatus(newStatus);
        propertyRepository.saveAndFlush(property);

    } catch (Exception e) {
        log.error("Error updating property status: ", e);
        throw new AppException(ErrorCode.INVALID_REQUEST);
    }
}

    // ==========================================
    // 4. XÓA MỀM (GỠ BÀI VI PHẠM)
    // ==========================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProperty(Long adminId, Long id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROPERTY_NOT_FOUND));

        // Admin chém thẳng tay, không cần check chủ sở hữu là ai
        propertyRepository.delete(property);
        log.info("Admin {} đã XÓA MỀM bài đăng vi phạm {}", adminId, id);
    }

    // ==========================================
    // 5. XEM THÙNG RÁC
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public Page<PropertyResponseDTO> getDeletedProperties(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return propertyRepository.findAllDeletedProperties(pageable).map(this::mapToResponse);
    }

    // ==========================================
    // 6. KHÔI PHỤC TỪ THÙNG RÁC
    // ==========================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreProperty(Long adminId, Long id) { // Giữ nguyên là void
        // Hứng kết quả trả về từ DB
        int affectedRows = propertyRepository.restoreByIdAdmin(id);

        // Nếu = 0 nghĩa là ID đó không nằm trong thùng rác
        if (affectedRows == 0) {
            throw new AppException(ErrorCode.PROPERTY_NOT_FOUND);
        }

        log.info("Admin {} đã KHÔI PHỤC bài đăng {}", adminId, id);
    }

    // ==========================================
    // 7. XÓA VĨNH VIỄN
    // ==========================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void hardDeleteProperty(Long adminId, Long id) { // Giữ nguyên là void
        // Hứng kết quả trả về từ DB
        int affectedRows = propertyRepository.hardDeleteByIdAdmin(id);

        // Nếu = 0 nghĩa là ID đó không nằm trong thùng rác
        if (affectedRows == 0) {
            throw new AppException(ErrorCode.PROPERTY_NOT_FOUND);
        }

        log.info("Admin {} đã XÓA VĨNH VIỄN bài đăng {}", adminId, id);
    }

    // ==========================================
    // HELPER: MAPPER
    // ==========================================
    private PropertyResponseDTO mapToResponse(Property property) {
        // Sếp copy logic map Entity sang DTO từ PropertyServiceImpl sang đây nhé.
        // Hoặc dùng MapStruct / ModelMapper nếu sếp có cài.
        PropertyResponseDTO dto = new PropertyResponseDTO();
        dto.setId(property.getId());
        dto.setTitle(property.getTitle());
        dto.setPrice(property.getPrice());
        dto.setStatus(property.getStatus().name());
        dto.setOwnerId(property.getOwnerId());
        // ... set các trường khác
        return dto;
    }
}