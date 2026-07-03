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
    private Integer categoryMatch;
    private Double area;
    private Double userArea;

    private Integer provinceMatch;
    private Integer districtMatch;
    private Integer wardMatch;
    private Integer streetMatch;

    private Integer transactionMatch;

    private Integer bedroomMatch;
    private Integer bathroomMatch;
    private Integer balconyMatch;

    private Integer furnishingMatch;
    private Integer availabilityMatch;

    private Double amenityMatchRatio;
}