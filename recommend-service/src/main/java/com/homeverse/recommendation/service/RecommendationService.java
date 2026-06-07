package com.homeverse.recommendation.service;

import com.homeverse.recommendation.client.MLClient;
import com.homeverse.recommendation.dto.FraudCheckResult;
import com.homeverse.recommendation.dto.PredictRequest;
import com.homeverse.recommendation.dto.PredictResponse;
import com.homeverse.recommendation.dto.TrackEventRequest;
import com.homeverse.recommendation.enums.FraudDecision;
import com.homeverse.recommendation.model.UserBehavior;
import com.homeverse.recommendation.repository.UserBehaviorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final SessionPreferenceService sessionPreferenceService;
    private final MLClient mlClient;
    private final UserBehaviorRepository userBehaviorRepository;
    private final UserInterestProfileService userInterestProfileService;
    private final FraudDetectionService fraudDetectionService;
    private final RecommendationMetricsService recommendationMetricsService;
    private final BanditService banditService;

    public PredictResponse predict(PredictRequest request) {
        try {
            recommendationMetricsService.getMlPredictCounter().increment();
            return mlClient.predict(request);
        } catch (Exception e) {
            log.warn("ML predict failed, use fallback. reason={}", e.getMessage());
            return buildFallbackResponse(request, "ml_unavailable");
        }
    }

    public PredictResponse track(TrackEventRequest request) {
        return recommendationMetricsService.getTrackTimer().record(() -> {
            FraudCheckResult fraudCheckResult = fraudDetectionService.check(
                    request.getUserId(),
                    request.getItemId(),
                    request.getItemType(),
                    request.getAction()
            );

            if (fraudCheckResult.getDecision() == FraudDecision.BLOCK) {
                recommendationMetricsService.getSpamBlockedCounter().increment();
                return buildBlockedResponse(request);
            }

            PredictRequest predictRequest = buildPredictRequest(request);

            PredictResponse response;
            try {
                response = mlClient.predict(predictRequest);
                recommendationMetricsService.getMlPredictCounter().increment();
            } catch (Exception e) {
                log.warn(
                        "ML service failed, keep tracking with fallback. userId={}, itemId={}, reason={}",
                        request.getUserId(),
                        request.getItemId(),
                        e.getMessage()
                );
                response = buildFallbackResponse(predictRequest, "fallback_rule");
            }

            recommendationMetricsService.getTrackCounter().increment();

            UserBehavior behavior = buildUserBehavior(
                    request,
                    predictRequest,
                    response,
                    fraudCheckResult
            );

            userBehaviorRepository.save(behavior);

            try {
                trackSessionDistrict(request);
            } catch (Exception e) {
                log.warn("Track district failed. userId={}, reason={}", request.getUserId(), e.getMessage());
            }

            try {
                recordBanditReward(request);
            } catch (Exception e) {
                log.warn("Bandit reward failed. userId={}, reason={}", request.getUserId(), e.getMessage());
            }

            try {
                userInterestProfileService.updateProfile(request.getUserId());
            } catch (Exception e) {
                log.warn("Update user profile failed. userId={}, reason={}", request.getUserId(), e.getMessage());
            }

            return response;
        });
    }

    private PredictResponse buildBlockedResponse(TrackEventRequest request) {
        PredictResponse response = new PredictResponse();

        response.setUserId(request.getUserId());
        response.setItemId(request.getItemId());
        response.setItemType(request.getItemType());
        response.setScore(0.0);
        response.setLabel("blocked_spam");

        return response;
    }

    private PredictResponse buildFallbackResponse(PredictRequest request, String label) {
        PredictResponse response = new PredictResponse();

        response.setUserId(request.getUserId());
        response.setItemId(request.getItemId());
        response.setItemType(request.getItemType());
        response.setScore(calculateFallbackScore(request));
        response.setLabel(label);

        return response;
    }

    private double calculateFallbackScore(PredictRequest request) {
        String action = request.getAction() == null ? "" : request.getAction().trim().toUpperCase();

        double actionScore = switch (action) {
            case "VIEW" -> 0.2;
            case "CLICK" -> 0.4;
            case "LIKE" -> 0.7;
            case "SAVE" -> 0.9;
            case "CONTACT" -> 1.0;
            default -> 0.1;
        };

        double duration = request.getDuration() == null || request.getDuration() <= 0
                ? 1.0
                : request.getDuration();

        double watchTime = request.getWatchTime() == null ? 0.0 : request.getWatchTime();
        double watchRatio = Math.min(1.0, Math.max(0.0, watchTime / duration));

        double locationMatch = request.getLocationMatch() == null ? 0.0 : request.getLocationMatch();
        double categoryMatch = request.getCategoryMatch() == null ? 0.0 : request.getCategoryMatch();

        double score =
                actionScore * 0.55 +
                        watchRatio * 0.25 +
                        locationMatch * 0.10 +
                        categoryMatch * 0.10;

        return Math.min(1.0, Math.max(0.0, score));
    }

    private PredictRequest buildPredictRequest(TrackEventRequest request) {
        PredictRequest predictRequest = new PredictRequest();

        predictRequest.setUserId(request.getUserId());
        predictRequest.setItemId(request.getItemId());
        predictRequest.setItemType(request.getItemType());
        predictRequest.setAction(request.getAction());
        predictRequest.setWatchTime(request.getWatchTime());
        predictRequest.setDuration(request.getDuration());
        predictRequest.setPrice(request.getPrice());
        predictRequest.setUserBudget(request.getUserBudget());
        predictRequest.setLocationMatch(request.getLocationMatch());
        predictRequest.setCategoryMatch(request.getCategoryMatch());

        return predictRequest;
    }

    private UserBehavior buildUserBehavior(
            TrackEventRequest request,
            PredictRequest predictRequest,
            PredictResponse response,
            FraudCheckResult fraudCheckResult
    ) {
        return UserBehavior.builder()
                .userId(request.getUserId())
                .itemId(request.getItemId())
                .itemType(request.getItemType())
                .action(request.getAction())
                .watchTime(predictRequest.getWatchTime())
                .duration(predictRequest.getDuration())
                .price(predictRequest.getPrice())
                .userBudget(predictRequest.getUserBudget())
                .locationMatch(predictRequest.getLocationMatch())
                .categoryMatch(predictRequest.getCategoryMatch())
                .score(response.getScore())
                .suspicious(fraudCheckResult.getDecision() == FraudDecision.SUSPICIOUS)
                .fraudReason(fraudCheckResult.getReason())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void trackSessionDistrict(TrackEventRequest request) {
        if (request.getDistrict() == null || request.getDistrict().isBlank()) {
            return;
        }

        sessionPreferenceService.trackDistrict(request.getUserId(), request.getDistrict());
    }

    private void recordBanditReward(TrackEventRequest request) {
        banditService.recordReward(
                request.getUserId(),
                request.getItemType(),
                request.getItemId(),
                request.getAction()
        );
    }
}