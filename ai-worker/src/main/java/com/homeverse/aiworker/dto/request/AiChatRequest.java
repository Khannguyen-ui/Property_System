package com.homeverse.aiworker.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest implements Serializable {
    private String userId;
    private String conversationId;
    private String userMessage;
}