package com.homeverse.aiworker.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeverse.aiworker.dto.response.AiChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiResponseProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;


    private static final String TOPIC_RESPONSE = "ai-responses";

    public void sendReply(AiChatResponse response) {
        try {
            String payload = objectMapper.writeValueAsString(response);

            // Dùng userId làm Key để Kafka đảm bảo thứ tự tin nhắn cho cùng 1 user
            kafkaTemplate.send(TOPIC_RESPONSE, response.getUserId(), payload);

            log.info(" [Kafka] Đã đẩy câu trả lời AI lên Topic {}: {}", TOPIC_RESPONSE, payload);
        } catch (Exception e) {
            log.error(" Lỗi khi gửi kết quả về Kafka: {}", e.getMessage());
        }
    }
}