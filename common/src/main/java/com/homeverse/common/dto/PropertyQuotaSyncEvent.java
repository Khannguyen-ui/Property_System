package com.homeverse.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyQuotaSyncEvent {
    private Long userId;
    private Integer freePostsRemaining;
    private String role;
    private String reason;
}