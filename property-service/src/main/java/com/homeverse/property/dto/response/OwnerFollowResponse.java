package com.homeverse.property.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OwnerFollowResponse {

    private Long ownerId;

    private Long followerId;

    private Boolean followed;

    private Long followerCount;
}