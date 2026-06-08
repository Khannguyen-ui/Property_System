package com.homeverse.property.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OwnerRatingSummaryResponse {

    private Long ownerId;

    private Double averageRating;
    private Long verifiedReviewCount;
    private Long reviewCount;
    private Long fiveStar;
    private Long fourStar;
    private Long threeStar;
    private Long twoStar;
    private Long oneStar;
}