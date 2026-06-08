package com.homeverse.property.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OwnerReviewResponse {

    private Long id;

    private Long ownerId;

    private Long reviewerId;

    private Long propertyId;

    private Integer rating;

    private String comment;

    private LocalDateTime createdAt;
}