package com.homeverse.chat.dto.ai;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AiChatRequest {
    private String userId;
    private String conversationId;
    private String userMessage;
}