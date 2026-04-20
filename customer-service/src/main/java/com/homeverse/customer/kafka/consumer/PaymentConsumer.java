package com.homeverse.customer.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeverse.common.dto.PaymentEvent;
import com.homeverse.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentConsumer {

    private final CustomerRepository customerRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-success-topic", groupId = "customer-upgrade-group-v5")
    public void consumeMembershipUpdate(String message) {
        try {
            log.info("🔥 ĐÃ CHẠM VÀO CONSUMER! Raw message: {}", message);

            PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);

            if (event == null || !"MEMBERSHIP".equals(event.getType())) {
                return;
            }

            log.info("📩 Nhận lệnh nâng cấp hội viên cho User ID: {}", event.getUserId());

            customerRepository.findById(event.getUserId()).ifPresentOrElse(customer -> {
                customer.setMembershipLevel(event.getPackageName());
                customer.setCurrentMembershipId(event.getPackageId());

                LocalDateTime now = LocalDateTime.now();
                if (customer.getMembershipExpiresAt() != null && customer.getMembershipExpiresAt().isAfter(now)) {
                    customer.setMembershipExpiresAt(customer.getMembershipExpiresAt().plusDays(event.getDurationDays()));
                } else {
                    customer.setMembershipExpiresAt(now.plusDays(event.getDurationDays()));
                }

                customerRepository.save(customer);
                log.info("✅ Đã cập nhật gói {} thành công cho {}", customer.getMembershipLevel(), customer.getFullName());

            }, () -> log.error("❌ Không tìm thấy Customer ID: {} để nâng cấp", event.getUserId()));

        } catch (Exception e) {
            log.error("❌ Lỗi xử lý tin nhắn Kafka: {}", e.getMessage());
        }
    }
}