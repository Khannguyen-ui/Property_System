package com.homeverse.recommendation.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendItemResponse {
    private String itemId;
    private Double score;
}