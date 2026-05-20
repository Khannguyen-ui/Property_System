package com.homeverse.recommendation.controller;

import com.homeverse.recommendation.dto.FeedItemResponse;
import com.homeverse.recommendation.dto.PredictRequest;
import com.homeverse.recommendation.dto.PredictResponse;
import com.homeverse.recommendation.service.RecommendationService;
import com.homeverse.recommendation.service.RedisRecommendationService;
import com.homeverse.recommendation.dto.PropertyResponseDTO;
import com.homeverse.recommendation.dto.TrackEventRequest;
import com.homeverse.recommendation.dto.PropertyReelResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recommend")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final RedisRecommendationService redisRecommendationService;

    @PostMapping("/predict")
    public PredictResponse predict(@RequestBody PredictRequest request) {
        return recommendationService.predict(request);
    }

   @GetMapping("/users/{userId}/properties")
public List<PropertyResponseDTO> getRecommendedProperties(@PathVariable Long userId) {
    return redisRecommendationService.getRecommendedProperties(userId);
}

@GetMapping("/users/{userId}/reels")
public List<PropertyReelResponseDTO> getRecommendedReels(@PathVariable Long userId) {
    return redisRecommendationService.getRecommendedReels(userId);
}
@PostMapping("/track")
public PredictResponse track(@RequestBody TrackEventRequest request) {
    return recommendationService.track(request);
}
@GetMapping("/users/{userId}/properties/final")
public List<PropertyResponseDTO> getFinalRecommendedProperties(@PathVariable Long userId) {
    return redisRecommendationService.getFinalRecommendedProperties(userId);
}

@GetMapping("/users/{userId}/reels/final")
public List<PropertyReelResponseDTO> getFinalRecommendedReels(@PathVariable Long userId) {
    return redisRecommendationService.getFinalRecommendedReels(userId);
}
}