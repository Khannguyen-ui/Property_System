package com.homeverse.recommendation.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictRequest {
    private Long userId;
    private Long itemId;
    private String itemType;
    private String action;
    private Double watchTime;
    private Double duration;
    private Double price;
    private Double userBudget;
    private Integer locationMatch;
    private Integer categoryMatch;
}