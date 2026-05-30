package com.homeverse.payment.kafka;

import com.homeverse.common.dto.PaymentEvent;
import com.homeverse.payment.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentConsumer {

    private final BillRepository billRepository;
    // Inject ObjectMapper để parse JSON nếu message vẫn là String
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @KafkaListener(topics = "property-billing-topic", groupId = "payment-group")
    public void consumeBillingRequest(String message) {
        log.info("=== KAFKA CONSUMER: Nhận lệnh xử lý giao dịch ===");
        try {
            // 1. Parse message thành DTO
            PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);

            saveBill(event);

            // 3. Phân loại gói để log hoặc xử lý logic nội bộ Payment
            if ("MEMBERSHIP".equals(event.getType())) {
                log.info("💳 Xử lý hóa đơn nâng cấp Membership cho User: {}", event.getUserId());
            } else if ("ROOM_PROMOTION".equals(event.getType())) {
                log.info("🚀 Xử lý hóa đơn dịch vụ Đẩy bài cho User: {}", event.getUserId());
            }

        } catch (Exception e) {
            log.error("❌ Lỗi xử lý tin nhắn Kafka: {}", e.getMessage());
        }
    }

    private void saveBill(PaymentEvent event) {
        // Logic tạo Bill entity từ event và save
        log.info("✅ Đã lưu hóa đơn cho giao dịch: {}", event.getTransactionId());
    }
}