package com.homeverse.recommendation.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OwnerRatingSummaryResponse {

    private Long ownerId;

    private Double averageRating;

    private Long reviewCount;
}