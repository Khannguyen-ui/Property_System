package com.homeverse.recommendation.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.homeverse.recommendation.dto.OwnerRatingTrackRequest;
import com.homeverse.recommendation.service.OwnerAffinityService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/recommend/owner-rating")
@RequiredArgsConstructor
public class OwnerRatingRecommendationController {

    private final OwnerAffinityService ownerAffinityService;

    @PostMapping("/track")
    public void trackOwnerRating(@RequestBody OwnerRatingTrackRequest request) {
        ownerAffinityService.trackRating(
                request.getUserId(),
                request.getOwnerId(),
                request.getRating()
        );
    }
}
