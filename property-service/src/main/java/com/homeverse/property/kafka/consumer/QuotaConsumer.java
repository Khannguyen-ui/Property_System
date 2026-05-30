package com.homeverse.property.kafka.consumer;

import com.homeverse.common.dto.PaymentEvent;
import com.homeverse.property.repository.OwnerQuotaRepository;
import com.homeverse.property.entity.OwnerQuota;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuotaConsumer {

    private final OwnerQuotaRepository ownerQuotaRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-success-topic", groupId = "property-quota-group")
    @Transactional // Đảm bảo tính nhất quán dữ liệu
    public void handleQuotaUpdate(String message) {
        log.info("🏠 [PROPERTY-SERVICE] Nhận tin nhắn cập nhật Quota...");
        try {
            PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);

            // Kiểm tra loại gói: Cả gói Membership và các gói nạp lượt lẻ đều dùng chung logic này
            if ("MEMBERSHIP".equals(event.getType())) {
                updateQuota(event);
            } else {
                log.info("⏩ Bỏ qua vì không phải gói Membership (Type: {})", event.getType());
            }

        } catch (Exception e) {
            log.error("❌ Lỗi xử lý Quota: {}", e.getMessage());
        }
    }

    private void updateQuota(PaymentEvent event) {
        // 1. Tìm bản ghi Quota của User
        OwnerQuota quota = ownerQuotaRepository.findById(event.getUserId())
                .orElseGet(() -> {
                    log.info("🆕 Tạo mới bản ghi Quota cho User ID: {}", event.getUserId());
                    return OwnerQuota.builder()
                            .ownerId(event.getUserId())
                            .freePostsRemaining(0)
                            .role("USER") // Mặc định là USER
                            .build();
                });

        int oldQuota = quota.getFreePostsRemaining() != null ? quota.getFreePostsRemaining() : 0;
        int addedQuota = event.getQuotaLimit() != null ? event.getQuotaLimit() : 0;

        // 2. Cập nhật số lượt đăng tin mới
        quota.setFreePostsRemaining(oldQuota + addedQuota);

        // 3. Logic nâng cấp Role (Nếu là gói xịn - priorityLevel cao)
        // Ví dụ: Nếu gói có priorityLevel > 0 thì lên thẳng OWNER_VIP
        if (event.getPriorityLevel() != null && event.getPriorityLevel() > 0) {
            quota.setRole("OWNER");
            log.info("💎 User {} đã được nâng cấp lên hạng VIP", event.getUserId());
        } else if ("USER".equals(quota.getRole())) {
            // Nếu là nạp lượt bình thường thì ít nhất cũng phải là OWNER để đăng bài
            quota.setRole("OWNER");
        }

        // 4. Lưu lại Database
        ownerQuotaRepository.save(quota);

        log.info("✅ CẬP NHẬT THÀNH CÔNG: User {} | Cũ: {} | Thêm: {} | Mới: {} | Role: {}", 
                event.getUserId(), oldQuota, addedQuota, quota.getFreePostsRemaining(), quota.getRole());
    }
}