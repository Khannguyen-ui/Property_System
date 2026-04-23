package com.homeverse.chat.dto.request;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    private String conversationId; // Thêm trường này để Mapper tìm được Conversation
    private Long senderId;
    private Long receiverId; 
    private String content;
    private String type; 
}