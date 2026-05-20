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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
        recommendationMetricsService.getMlPredictCounter().increment();
        return mlClient.predict(request);
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
            PredictResponse response = mlClient.predict(predictRequest);

            recommendationMetricsService.getTrackCounter().increment();
            recommendationMetricsService.getMlPredictCounter().increment();

            UserBehavior behavior = buildUserBehavior(
                    request,
                    predictRequest,
                    response,
                    fraudCheckResult
            );

            userBehaviorRepository.save(behavior);
            trackSessionDistrict(request);
            recordBanditReward(request);
            userInterestProfileService.updateProfile(request.getUserId());

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

    private PredictRequest buildPredictRequest(TrackEventRequest request) {
        PredictRequest predictRequest = new PredictRequest();

        predictRequest.setUserId(request.getUserId());
        predictRequest.setItemId(request.getItemId());
        predictRequest.setItemType(request.getItemType());
        predictRequest.setAction(request.getAction());
        predictRequest.setWatchTime(defaultDouble(request.getWatchTime()));
        predictRequest.setDuration(defaultDouble(request.getDuration()));
        predictRequest.setPrice(defaultDouble(request.getPrice()));
        predictRequest.setUserBudget(defaultDouble(request.getUserBudget()));
        predictRequest.setLocationMatch(defaultInteger(request.getLocationMatch()));
        predictRequest.setCategoryMatch(defaultInteger(request.getCategoryMatch()));

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
                .suspicious(false)
                .fraudReason(null)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void trackSessionDistrict(TrackEventRequest request) {
        if (request.getDistrict() == null || request.getDistrict().isBlank()) {
            return;
        }

        sessionPreferenceService.trackDistrict(
                request.getUserId(),
                request.getDistrict()
        );
    }

    private void recordBanditReward(TrackEventRequest request) {
        banditService.recordReward(
                request.getUserId(),
                request.getItemType(),
                request.getItemId(),
                request.getAction()
        );
    }

    private Double defaultDouble(Double value) {
        return value != null ? value : 0.0;
    }

    private Integer defaultInteger(Integer value) {
        return value != null ? value : 0;
    }
}