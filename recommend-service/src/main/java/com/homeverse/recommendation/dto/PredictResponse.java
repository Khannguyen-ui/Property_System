package com.homeverse.recommendation.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictResponse {
    private Long userId;
    private Long itemId;
    private String itemType;
    private Double score;
    private String label;
}