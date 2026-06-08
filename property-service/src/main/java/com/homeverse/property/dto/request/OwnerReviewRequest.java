package com.homeverse.property.dto.request;

import java.util.List;

import lombok.Data;

@Data
public class OwnerReviewRequest {

    private Long ownerId;

    private Long propertyId;

    private Integer rating;
    private List<String> images;
    private String comment;
}