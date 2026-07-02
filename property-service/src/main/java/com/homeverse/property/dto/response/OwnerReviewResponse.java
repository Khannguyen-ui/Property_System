package com.homeverse.property.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OwnerReviewResponse {

    private Long id;

    private Long ownerId;

    private Long propertyId;
    private Boolean verified;
    private Integer rating;
    private List<String> images;
    private String comment;
    private String ownerReply;
    private LocalDateTime ownerReplyAt;
    private Long reviewerId;
    private String reviewerName;
    private String reviewerAvatar;
    private LocalDateTime createdAt;
}