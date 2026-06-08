package com.homeverse.recommendation.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SourceCTRResponse {

    private String source;

    private Long impressions;

    private Long clicks;

    private Long contacts;

    private Double ctr;

    private Double contactRate;
}