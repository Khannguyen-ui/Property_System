package com.homeverse.chat.dto.ai;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AiChatResponse {
    private String userId;
    private String conversationId;
    private String aiReply;
    private String status;
}