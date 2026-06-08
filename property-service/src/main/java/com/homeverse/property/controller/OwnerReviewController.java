package com.homeverse.property.controller;

import com.homeverse.property.dto.request.OwnerReviewReplyRequest;
import com.homeverse.property.dto.request.OwnerReviewRequest;
import com.homeverse.property.dto.response.OwnerRatingSummaryResponse;
import com.homeverse.property.dto.response.OwnerReviewResponse;
import com.homeverse.property.service.OwnerReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owners/reviews")
@RequiredArgsConstructor
public class OwnerReviewController {

    private final OwnerReviewService ownerReviewService;

    @PostMapping
    public ResponseEntity<OwnerReviewResponse> reviewOwner(
            @RequestHeader("X-User-Id") Long reviewerId,
            @RequestBody OwnerReviewRequest request
    ) {
        return ResponseEntity.ok(
                ownerReviewService.reviewOwner(reviewerId, request)
        );
    }

    @GetMapping("/{ownerId}")
    public ResponseEntity<List<OwnerReviewResponse>> getOwnerReviews(
            @PathVariable Long ownerId
    ) {
        return ResponseEntity.ok(
                ownerReviewService.getOwnerReviews(ownerId)
        );
    }

    @GetMapping("/{ownerId}/summary")
    public ResponseEntity<OwnerRatingSummaryResponse> getSummary(
            @PathVariable Long ownerId
    ) {
        return ResponseEntity.ok(
                ownerReviewService.getOwnerRatingSummary(ownerId)
        );
    }
    @PostMapping("/{reviewId}/reply")
public ResponseEntity<OwnerReviewResponse> replyReview(
        @RequestHeader("X-User-Id") Long ownerId,
        @PathVariable Long reviewId,
        @RequestBody OwnerReviewReplyRequest request
) {
    return ResponseEntity.ok(
            ownerReviewService.replyReview(ownerId, reviewId, request.getReply())
    );
}
}