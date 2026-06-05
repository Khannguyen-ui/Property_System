package com.homeverse.property.service;

import java.util.List;

import com.homeverse.property.dto.response.OwnerFollowResponse;

public interface OwnerFollowService {

    OwnerFollowResponse toggleFollow(Long followerId, Long ownerId);

    boolean isFollowing(Long followerId, Long ownerId);

    long countFollowers(Long ownerId);

    long countFollowing(Long followerId);
    List<Long> getFollowedOwnerIds(Long followerId);
}