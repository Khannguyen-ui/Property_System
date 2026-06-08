package com.homeverse.property.service.impl;

import com.homeverse.property.config.RecommendClient;
import com.homeverse.property.dto.request.OwnerRatingTrackRequest;
import com.homeverse.property.dto.request.OwnerReviewRequest;
import com.homeverse.property.dto.response.OwnerRatingSummaryResponse;
import com.homeverse.property.dto.response.OwnerReviewResponse;
import com.homeverse.property.entity.OwnerReview;
import com.homeverse.property.repository.OwnerReviewRepository;
import com.homeverse.property.service.OwnerReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OwnerReviewServiceImpl implements OwnerReviewService {
    private final RecommendClient recommendClient;
    private final OwnerReviewRepository ownerReviewRepository;

    @Override
    public OwnerReviewResponse reviewOwner(
            Long reviewerId,
            OwnerReviewRequest request
    ) {
        if (reviewerId == null) {
            throw new RuntimeException("Bạn cần đăng nhập để đánh giá");
        }

        if (request.getOwnerId() == null) {
            throw new RuntimeException("ownerId không hợp lệ");
        }

        if (reviewerId.equals(request.getOwnerId())) {
            throw new RuntimeException("Bạn không thể tự đánh giá chính mình");
        }

        if (request.getRating() == null
                || request.getRating() < 1
                || request.getRating() > 5) {
            throw new RuntimeException("Rating phải từ 1 đến 5");
        }

        OwnerReview review = ownerReviewRepository
                .findByOwnerIdAndReviewerIdAndPropertyId(
                        request.getOwnerId(),
                        reviewerId,
                        request.getPropertyId()
                )
                .orElseGet(OwnerReview::new);

        review.setOwnerId(request.getOwnerId());
        review.setReviewerId(reviewerId);
        review.setPropertyId(request.getPropertyId());
        review.setRating(request.getRating());
        review.setComment(
                request.getComment() != null
                        ? request.getComment().trim()
                        : null
        );

        OwnerReview saved = ownerReviewRepository.save(review);

        return toResponse(saved);
    }

    @Override
    public List<OwnerReviewResponse> getOwnerReviews(Long ownerId) {
        return ownerReviewRepository
                .findByOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public OwnerRatingSummaryResponse getOwnerRatingSummary(Long ownerId) {
        Double avg = ownerReviewRepository.getAverageRating(ownerId);
        long count = ownerReviewRepository.countByOwnerId(ownerId);

        return OwnerRatingSummaryResponse.builder()
                .ownerId(ownerId)
                .averageRating(avg != null ? avg : 0.0)
                .reviewCount(count)
                .build();
    }

    public OwnerReviewResponse toResponse(OwnerReview review) {
        return OwnerReviewResponse.builder()
                .id(review.getId())
                .ownerId(review.getOwnerId())
                .reviewerId(review.getReviewerId())
                .propertyId(review.getPropertyId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
    public void trackOwnerRating(Long reviewerId, Long ownerId, Integer rating) {
    try {
        if (reviewerId == null || ownerId == null || rating == null) {
            return;
        }

        recommendClient.trackOwnerRating(
                OwnerRatingTrackRequest.builder()
                        .userId(reviewerId)
                        .ownerId(ownerId)
                        .rating(rating)
                        .build()
        );

    } catch (Exception e) {
        e.printStackTrace();
    }
}
}