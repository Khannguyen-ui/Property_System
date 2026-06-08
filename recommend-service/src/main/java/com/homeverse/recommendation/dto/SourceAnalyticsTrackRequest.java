package com.homeverse.recommendation.dto;

import lombok.Data;

@Data
public class SourceAnalyticsTrackRequest {

    private Long userId;

    private Long itemId;

    private String itemType;

    private String source;

    private String eventType;
}