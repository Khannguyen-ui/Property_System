package com.homeverse.chat.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeverse.chat.dto.ai.AiChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AiRequestProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public AiRequestProducer(
            @Qualifier("aiStringKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendAiRequest(String key, AiChatRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);

            kafkaTemplate.send("ai-requests", key, json);

            log.info("[Kafka] Đã gửi AI request: {}", json);
        } catch (Exception e) {
            log.error("Lỗi gửi AI request sang ai-worker", e);
        }
    }
}