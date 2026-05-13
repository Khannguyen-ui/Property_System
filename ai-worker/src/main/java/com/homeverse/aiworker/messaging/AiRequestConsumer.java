package com.homeverse.aiworker.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeverse.aiworker.dto.request.AiChatRequest;
import com.homeverse.aiworker.service.RagOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiRequestConsumer {

    private final ObjectMapper objectMapper;
    private final RagOrchestratorService ragOrchestratorService;

    // Lắng nghe từ Topic "ai-requests"
    @KafkaListener(topics = "ai-requests", groupId = "ai-worker-group")
    public void listen(String message) {
        try {
            log.info(" [Kafka] Nhận yêu cầu AI mới từ Gateway: {}", message);

            // Giải mã JSON thành Object
            AiChatRequest request = objectMapper.readValue(message, AiChatRequest.class);

            // Đẩy sang cho Nhạc trưởng RAG xử lý
            ragOrchestratorService.processAndReply(request);

        } catch (Exception e) {
            log.error(" Lỗi giải mã tin nhắn Kafka: {}", e.getMessage());
        }
    }
}