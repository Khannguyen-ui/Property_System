package com.homeverse.aiworker.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LastSearchContextDTO {
    private String userId;
    private String userName;
    private String conversationId;
    private String lastUserQuery;
    private List<Long> lastPropertyIds;
    private Integer totalMatched;
    private Boolean hasMore;
}