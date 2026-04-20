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
    private Long senderId;
    private Long receiverId; // Người nhận (Quan trọng)
    private String content;
    private String type; // TEXT, IMAGE
}