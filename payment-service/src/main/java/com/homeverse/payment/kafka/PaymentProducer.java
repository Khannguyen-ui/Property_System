package com.homeverse.payment.kafka;

import com.homeverse.common.dto.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProducer {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentProducer.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "payment-success-topic";

    public void sendPaymentSuccess(PaymentEvent event) {
        log.info("=== KAFKA PRODUCER: Đang bắn tin nhắn thanh toán cho User {} ===", event.getUserId());
        try {
            // Gửi Object sang Kafka, Spring sẽ tự biến nó thành JSON
            kafkaTemplate.send(TOPIC, event);
            log.info("Bắn tin thành công mã GD: {}", event.getTransactionId());
        } catch (Exception e) {
            log.error("Lỗi khi bắn tin Kafka: {}", e.getMessage());
        }
    }
}