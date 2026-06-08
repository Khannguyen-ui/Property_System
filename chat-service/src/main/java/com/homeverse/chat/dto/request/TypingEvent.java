package com.homeverse.chat.dto.request;

import lombok.Data;

@Data
public class TypingEvent {
    private Long senderId;
    private Long receiverId;
    private String conversationId;
    private boolean typing;
}