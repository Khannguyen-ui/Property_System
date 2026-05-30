package com.homeverse.recommendation.service;

import com.homeverse.recommendation.model.RankingConfig;
import com.homeverse.recommendation.repository.RankingConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RankingConfigService {

    private static final String DEFAULT_ID = "DEFAULT";

    private final RankingConfigRepository rankingConfigRepository;

    public RankingConfig getConfig() {
        return rankingConfigRepository
                .findById(DEFAULT_ID)
                .orElseGet(this::createDefaultConfig);
    }

    public RankingConfig updateConfig(RankingConfig request) {
        RankingConfig current = getConfig();

        current.setPromotedBoost(request.getPromotedBoost());
        current.setDistrictBoost(request.getDistrictBoost());
        current.setFavoriteTypeBoost(request.getFavoriteTypeBoost());
        current.setFavoriteActionBoost(request.getFavoriteActionBoost());
        current.setOwnerTrustWeight(request.getOwnerTrustWeight());
        current.setFreshOneHourBoost(request.getFreshOneHourBoost());
        current.setFreshOneDayBoost(request.getFreshOneDayBoost());
        current.setFreshThreeDaysBoost(request.getFreshThreeDaysBoost());
        current.setPromotedLimit(request.getPromotedLimit());
        current.setBehaviorLimit(request.getBehaviorLimit());
        current.setCollaborativeLimit(request.getCollaborativeLimit());
        current.setTrendingLimit(request.getTrendingLimit());
        current.setRandomLimit(request.getRandomLimit());
        current.setFinalLimit(request.getFinalLimit());

        return rankingConfigRepository.save(current);
    }

    private RankingConfig createDefaultConfig() {
        RankingConfig config = RankingConfig.builder()
                .id(DEFAULT_ID)
                .promotedBoost(2.0)
                .districtBoost(0.5)
                .favoriteTypeBoost(0.3)
                .favoriteActionBoost(0.15)
                .ownerTrustWeight(0.3)
                .freshOneHourBoost(0.5)
                .freshOneDayBoost(0.2)
                .freshThreeDaysBoost(0.1)
                .promotedLimit(10)
                .behaviorLimit(20)
                .collaborativeLimit(20)
                .trendingLimit(20)
                .randomLimit(100)
                .finalLimit(50)
                .build();

        return rankingConfigRepository.save(config);
    }
}