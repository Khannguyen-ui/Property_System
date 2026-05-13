package com.homeverse.aiworker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse implements Serializable {
    private String userId;
    private String conversationId;
    private String aiReply;
    private String status;
    private List<PropertyCardDTO> items;
}