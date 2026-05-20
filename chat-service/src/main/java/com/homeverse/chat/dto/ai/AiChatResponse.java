package com.homeverse.chat.dto.ai;
import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AiChatResponse {
    private String userId;
    private String conversationId;
    private String aiReply;
    private String status;
    private List<PropertyCardDTO> items;
    private Integer totalMatched;
    private Boolean hasMore;
}