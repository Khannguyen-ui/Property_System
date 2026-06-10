package com.homeverse.chat.dto.request;

import lombok.Data;

@Data
public class MessageReactionRequest {
    private String messageId;
    private String emoji;
}