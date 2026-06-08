package com.homeverse.property.dto.request;

import lombok.Data;

@Data
public class OwnerReviewRequest {

    private Long ownerId;

    private Long propertyId;

    private Integer rating;

    private String comment;
}