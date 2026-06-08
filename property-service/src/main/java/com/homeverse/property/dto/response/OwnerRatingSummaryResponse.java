package com.homeverse.property.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OwnerRatingSummaryResponse {

    private Long ownerId;

    private Double averageRating;

    private Long reviewCount;
}