package com.homeverse.recommendation.controller;

import com.homeverse.recommendation.dto.AnalyticsDashboardResponse;
import com.homeverse.recommendation.service.AnalyticsService;
import com.homeverse.recommendation.service.BanditService;
import com.homeverse.recommendation.service.CollaborativeRecommendationService;
import com.homeverse.recommendation.service.TrendingRecommendationService;
import com.homeverse.recommendation.dto.BanditArmStats;
import com.homeverse.recommendation.service.BanditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.homeverse.recommendation.dto.PropertyResponseDTO;
import com.homeverse.recommendation.dto.PropertyReelResponseDTO;
import com.homeverse.recommendation.service.TrendingRecommendationService;
import com.homeverse.recommendation.service.CollaborativeRecommendationService;
import java.util.List;

@RestController
@RequestMapping("/recommend/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final BanditService banditService;
    private final CollaborativeRecommendationService collaborativeRecommendationService;
    private final TrendingRecommendationService trendingRecommendationService;
    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public AnalyticsDashboardResponse getDashboard() {
        return analyticsService.getDashboard();
    }

    @GetMapping("/trending/properties")
    public List<PropertyResponseDTO> getTrendingProperties() {
        return trendingRecommendationService.getTrendingProperties();
    }

    @GetMapping("/trending/reels")
    public List<PropertyReelResponseDTO> getTrendingReels() {
        return trendingRecommendationService.getTrendingReels();
    }

    @GetMapping("/collaborative/users/{userId}/properties")
    public List<PropertyResponseDTO> getCollaborativeProperties(@PathVariable Long userId) {
        return collaborativeRecommendationService.getCollaborativeProperties(userId);
    }

    @GetMapping("/collaborative/users/{userId}/reels")
    public List<PropertyReelResponseDTO> getCollaborativeReels(@PathVariable Long userId) {
        return collaborativeRecommendationService.getCollaborativeReels(userId);
    }

    @GetMapping("/bandit/users/{userId}")
    public List<BanditArmStats> getBanditStats(
            @PathVariable Long userId) {
        return banditService.getStats(userId);
    }
}