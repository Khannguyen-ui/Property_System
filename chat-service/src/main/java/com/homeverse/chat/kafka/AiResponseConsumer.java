package com.homeverse.chat.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeverse.chat.dto.ai.AiChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiResponseConsumer {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(
            topics = "ai-responses",
            groupId = "chat-service-ai-response-group",
            containerFactory = "aiStringKafkaListenerContainerFactory"
    )
    public void listenAiResponse(String message) {
        try {
            log.info("Nhận câu trả lời từ AI Worker: {}", message);

            AiChatResponse response = objectMapper.readValue(message, AiChatResponse.class);

            log.info("AI response parsed: userId={}, conversationId={}, status={}, itemsCount={}, totalMatched={}, hasMore={}, items={}",
                    response.getUserId(),
                    response.getConversationId(),
                    response.getStatus(),
                    response.getItems() == null ? 0 : response.getItems().size(),
                    response.getTotalMatched(),
                    response.getHasMore(),
                    response.getItems());

            messagingTemplate.convertAndSend(
                    "/topic/user/" + response.getUserId() + "/ai",
                    response
            );
        } catch (Exception e) {
            log.error("Lỗi khi đọc Kafka message từ AI", e);
        }
    }
}