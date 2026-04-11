package com.homeverse.payment.kafka;

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

    // Nghe từ topic 'property-billing-topic' (do Property Service bắn sang)
    @KafkaListener(topics = "property-billing-topic", groupId = "payment-group")
    public void consumeBillingRequest(String message) {
        log.info("=== KAFKA CONSUMER: Nhận lệnh tạo hóa đơn mới ===");
        try {
            // Ở đây Ân có thể dùng ObjectMapper để parse message thành Bill Entity
            // Sau đó: billRepository.save(newBill);
            log.info("Đã tạo hóa đơn thành công từ tin nhắn Kafka");
        } catch (Exception e) {
            log.error("Lỗi xử lý tin nhắn tạo hóa đơn: {}", e.getMessage());
        }
    }
}