package com.homeverse.payment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeverse.common.dto.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProducer {
    // Nhớ đổi thành KafkaTemplate<String, String>
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String TOPIC = "payment-success-topic";

    public void sendPaymentSuccess(PaymentEvent event) {
        log.info("=== KAFKA PRODUCER: Đang bắn tin cho User {} ===", event.getUserId());
        try {
            // Chuyển Object thành String JSON
            String message = objectMapper.writeValueAsString(event);
            
            // THÊM .get() - ĐÂY LÀ CHÌA KHÓA
            kafkaTemplate.send(TOPIC, message).get(); 
            
            log.info("✅ XÁC NHẬN: Tin nhắn đã thực sự bay lên Kafka!");
        } catch (Exception e) {
            // Nếu có lỗi kết nối tới kafka:29092, nó sẽ hiện ở đây ngay lập tức
            log.error("❌ LỖI GỬI KAFKA: {}", e.getMessage(), e);
        }
    }
}