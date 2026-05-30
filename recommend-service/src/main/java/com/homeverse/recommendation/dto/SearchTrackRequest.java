package com.homeverse.recommendation.dto;

import lombok.Data;

@Data
public class SearchTrackRequest {

    private Long userId;

    private String keyword;
}