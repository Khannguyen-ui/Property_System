package com.homeverse.property.service;

import com.homeverse.property.dto.request.OwnerReviewRequest;
import com.homeverse.property.dto.response.OwnerRatingSummaryResponse;
import com.homeverse.property.dto.response.OwnerReviewResponse;

import java.util.List;

public interface OwnerReviewService {

    OwnerReviewResponse reviewOwner(Long reviewerId, OwnerReviewRequest request);

    List<OwnerReviewResponse> getOwnerReviews(Long ownerId);

    OwnerRatingSummaryResponse getOwnerRatingSummary(Long ownerId);
    void trackOwnerRating(Long reviewerId, Long ownerId, Integer rating);
    public OwnerReviewResponse replyReview(Long ownerId, Long reviewId, String reply);
}