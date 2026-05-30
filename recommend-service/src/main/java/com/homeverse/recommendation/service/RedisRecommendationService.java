package com.homeverse.recommendation.service;

import com.homeverse.common.dto.ApiResponse;
import com.homeverse.recommendation.client.PropertyClient;
import com.homeverse.recommendation.dto.PropertyReelResponseDTO;
import com.homeverse.recommendation.dto.PropertyResponseDTO;
import com.homeverse.recommendation.enums.RecommendationArm;
import com.homeverse.recommendation.model.RankingConfig;
import com.homeverse.recommendation.model.UserInterestProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisRecommendationService {

    private static final int REDIS_FETCH_LIMIT = 100;
    private static final int MIN_SOURCE_LIMIT = 10;

    private final BanditService banditService;
    private final TrendingRecommendationService trendingRecommendationService;
    private final SessionPreferenceService sessionPreferenceService;
    private final StringRedisTemplate redisTemplate;
    private final PropertyClient propertyClient;
    private final FinalRankingService finalRankingService;
    private final UserInterestProfileService userInterestProfileService;
    private final CollaborativeRecommendationService collaborativeRecommendationService;
    private final RankingConfigService rankingConfigService;

    public List<PropertyResponseDTO> getRecommendedProperties(Long userId) {
        String key = "user:" + userId + ":recommend:properties";

        Set<ZSetOperations.TypedTuple<String>> items = redisTemplate.opsForZSet().reverseRangeWithScores(
                key,
                0,
                REDIS_FETCH_LIMIT);

        if (items == null || items.isEmpty()) {
            return List.of();
        }

        List<PropertyResponseDTO> result = new ArrayList<>();

        for (ZSetOperations.TypedTuple<String> item : items) {
            if (item.getValue() == null) {
                continue;
            }

            try {
                Long propertyId = Long.valueOf(item.getValue());
                PropertyResponseDTO property = propertyClient.getPropertyById(propertyId);

                result.add(
                        PropertyResponseDTO.builder()
                                .id(property.getId())
                                .itemType("property")
                                .score(item.getScore())
                                .title(property.getTitle())
                                .price(property.getPrice())
                                .address(property.getAddress())
                                .district(property.getDistrict())
                                .videoUrl(property.getVideoUrl())
                                .ownerId(property.getOwnerId())
                                .ownerTrustScore(getOwnerTrustScore(property.getOwnerId()))
                                .createdAt(property.getCreatedAt())
                                .isPromoted(property.getIsPromoted())
                                .build());
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    public List<PropertyReelResponseDTO> getRecommendedReels(Long userId) {
        String key = "user:" + userId + ":recommend:reels";

        Set<ZSetOperations.TypedTuple<String>> items = redisTemplate.opsForZSet().reverseRangeWithScores(
                key,
                0,
                REDIS_FETCH_LIMIT);

        if (items == null || items.isEmpty()) {
            return List.of();
        }

        List<PropertyReelResponseDTO> result = new ArrayList<>();

        for (ZSetOperations.TypedTuple<String> item : items) {
            if (item.getValue() == null) {
                continue;
            }

            try {
                Long reelId = Long.valueOf(item.getValue());

                ApiResponse<PropertyReelResponseDTO> response = propertyClient.getReelById(reelId);

                if (response == null || response.getResult() == null) {
                    continue;
                }

                PropertyReelResponseDTO reel = response.getResult();

                result.add(
                        PropertyReelResponseDTO.builder()
                                .id(reel.getId())
                                .itemType("reel")
                                .score(item.getScore())
                                .title(reel.getTitle())
                                .price(reel.getPrice())
                                .address(reel.getAddress())
                                .videoUrl(reel.getVideoUrl())
                                .isLiked(reel.isLiked())
                                .isSaved(reel.isSaved())
                                .likeCount(reel.getLikeCount())
                                .ownerSlug(reel.getOwnerSlug())
                                .ownerNameSnapshot(reel.getOwnerNameSnapshot())
                                .ownerAvatarSnapshot(reel.getOwnerAvatarSnapshot())
                                .createdAt(reel.getCreatedAt())
                                .isPromoted(reel.getIsPromoted())
                                .build());
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    public List<PropertyResponseDTO> getFinalRecommendedProperties(Long userId) {
        RankingConfig config = rankingConfigService.getConfig();

        Map<RecommendationArm, Double> weights = banditService.getWeights(userId);

        int finalLimit = safeLimit(config.getFinalLimit(), 50);
        int promotedLimit = safeLimit(config.getPromotedLimit(), 5);
        int remainingSlots = Math.max(finalLimit - promotedLimit, finalLimit);

        int behaviorLimit = calculateDynamicLimit(
                remainingSlots,
                weights,
                RecommendationArm.BEHAVIOR);

        int collaborativeLimit = calculateDynamicLimit(
                remainingSlots,
                weights,
                RecommendationArm.COLLABORATIVE);

        int trendingLimit = calculateDynamicLimit(
                remainingSlots,
                weights,
                RecommendationArm.TRENDING);

        int randomLimit = Math.max(
                calculateDynamicLimit(
                        remainingSlots,
                        weights,
                        RecommendationArm.RANDOM),
                finalLimit);

        List<PropertyResponseDTO> promoted = propertyClient.getPromotedProperties();

        List<PropertyResponseDTO> behavior = getRecommendedProperties(userId);

        List<PropertyResponseDTO> collaborative = collaborativeRecommendationService.getCollaborativeProperties(userId);

        List<PropertyResponseDTO> trending = trendingRecommendationService.getTrendingProperties();

        if (trending.isEmpty()) {
            trending = propertyClient.getTrendingProperties();
        }

        List<PropertyResponseDTO> random = propertyClient.getRandomProperties();

        Map<Long, PropertyResponseDTO> result = new LinkedHashMap<>();

        addProperties(
                result,
                promoted,
                promotedLimit,
                "property",
                1.0,
                null,
                userId);

        addProperties(
                result,
                behavior,
                behaviorLimit,
                "property",
                null,
                RecommendationArm.BEHAVIOR,
                userId);

        addProperties(
                result,
                collaborative,
                collaborativeLimit,
                "property",
                null,
                RecommendationArm.COLLABORATIVE,
                userId);

        addProperties(
                result,
                trending,
                trendingLimit,
                "property",
                0.6,
                RecommendationArm.TRENDING,
                userId);

        addProperties(
                result,
                random,
                randomLimit,
                "property",
                0.3,
                RecommendationArm.RANDOM,
                userId);

        if (result.size() < finalLimit) {
            addProperties(
                    result,
                    propertyClient.getRandomProperties(),
                    finalLimit,
                    "property",
                    0.2,
                    RecommendationArm.RANDOM,
                    userId);
            int retry = 0;

            while (result.size() < finalLimit && retry < 10) {
                addProperties(
                        result,
                        propertyClient.getRandomProperties(),
                        finalLimit,
                        "property",
                        0.2,
                        RecommendationArm.RANDOM,
                        userId);

                retry++;
            }
        }

        List<PropertyResponseDTO> combined = new ArrayList<>(result.values());

        String preferredDistrict = sessionPreferenceService.getFavoriteDistrict(userId);

        if (preferredDistrict == null) {
            preferredDistrict = extractPreferredDistrict(behavior);
        }

        UserInterestProfile profile = userInterestProfileService.getProfile(userId);
        System.out.println("PROMOTED SIZE = " + promoted.size());
        System.out.println("BEHAVIOR SIZE = " + behavior.size());
        System.out.println("COLLAB SIZE = " + collaborative.size());
        System.out.println("TRENDING SIZE = " + trending.size());
        System.out.println("RANDOM SIZE = " + random.size());
        System.out.println("RESULT SIZE = " + result.size());
         System.out.println("COMBINED SIZE = " + combined.size());
        return finalRankingService.rankProperties(
            combined,
            preferredDistrict,
            profile);
    }

    public List<PropertyReelResponseDTO> getFinalRecommendedReels(Long userId) {
        RankingConfig config = rankingConfigService.getConfig();

        Map<RecommendationArm, Double> weights = banditService.getWeights(userId);

        int finalLimit = safeLimit(config.getFinalLimit(), 50);
        int promotedLimit = safeLimit(config.getPromotedLimit(), 5);
        int remainingSlots = Math.max(finalLimit - promotedLimit, finalLimit);

        int behaviorLimit = calculateDynamicLimit(
                remainingSlots,
                weights,
                RecommendationArm.BEHAVIOR);

        int collaborativeLimit = calculateDynamicLimit(
                remainingSlots,
                weights,
                RecommendationArm.COLLABORATIVE);

        int trendingLimit = calculateDynamicLimit(
                remainingSlots,
                weights,
                RecommendationArm.TRENDING);

        int randomLimit = Math.max(
                calculateDynamicLimit(
                        remainingSlots,
                        weights,
                        RecommendationArm.RANDOM),
                finalLimit);

        List<PropertyReelResponseDTO> promoted = getResultList(propertyClient.getPromotedReels());

        List<PropertyReelResponseDTO> behavior = getRecommendedReels(userId);

        List<PropertyReelResponseDTO> collaborative = collaborativeRecommendationService.getCollaborativeReels(userId);

        List<PropertyReelResponseDTO> trending = trendingRecommendationService.getTrendingReels();

        if (trending.isEmpty()) {
            trending = getResultList(propertyClient.getTrendingReels());
        }

        List<PropertyReelResponseDTO> random = getResultList(propertyClient.getRandomReels());

        Map<Long, PropertyReelResponseDTO> result = new LinkedHashMap<>();

        addReels(
                result,
                promoted,
                promotedLimit,
                "reel",
                1.0,
                null,
                userId);

        addReels(
                result,
                behavior,
                behaviorLimit,
                "reel",
                null,
                RecommendationArm.BEHAVIOR,
                userId);

        addReels(
                result,
                collaborative,
                collaborativeLimit,
                "reel",
                null,
                RecommendationArm.COLLABORATIVE,
                userId);

        addReels(
                result,
                trending,
                trendingLimit,
                "reel",
                0.6,
                RecommendationArm.TRENDING,
                userId);

        addReels(
                result,
                random,
                randomLimit,
                "reel",
                0.3,
                RecommendationArm.RANDOM,
                userId);

        if (result.size() < finalLimit) {
            addReels(
                    result,
                    getResultList(propertyClient.getRandomReels()),
                    finalLimit,
                    "reel",
                    0.2,
                    RecommendationArm.RANDOM,
                    userId);
            int retry = 0;

            while (result.size() < finalLimit && retry < 10) {
                addReels(
                        result,
                        getResultList(propertyClient.getRandomReels()),
                        finalLimit,
                        "reel",
                        0.2,
                        RecommendationArm.RANDOM,
                        userId);

                retry++;
            }
        }

        List<PropertyReelResponseDTO> combined = new ArrayList<>(result.values());

        String preferredDistrict = sessionPreferenceService.getFavoriteDistrict(userId);

        if (preferredDistrict == null) {
            preferredDistrict = extractPreferredDistrictFromReels(behavior);
        }

        UserInterestProfile profile = userInterestProfileService.getProfile(userId);

        return finalRankingService.rankReels(
                combined,
                preferredDistrict,
                profile);
    }

    private void addProperties(
            Map<Long, PropertyResponseDTO> result,
            List<PropertyResponseDTO> items,
            Integer limit,
            String itemType,
            Double score,
            RecommendationArm arm,
            Long userId) {
        if (items == null || items.isEmpty()) {
            return;
        }

        int safeLimit = safeLimit(limit, MIN_SOURCE_LIMIT);

        items.stream()
                .limit(safeLimit)
                .forEach(item -> {
                    if (item.getId() == null) {
                        return;
                    }

                    item.setItemType(itemType);

                    if (item.getScore() == null && score != null) {
                        item.setScore(score);
                    }

                    if (arm != null) {
                        item.setReasons(
                                List.of("BANDIT_" + arm.name()));

                        banditService.registerImpression(
                                userId,
                                itemType,
                                item.getId(),
                                arm);
                    }

                    result.putIfAbsent(
                            item.getId(),
                            item);
                });
    }

    private void addReels(
            Map<Long, PropertyReelResponseDTO> result,
            List<PropertyReelResponseDTO> items,
            Integer limit,
            String itemType,
            Double score,
            RecommendationArm arm,
            Long userId) {
        if (items == null || items.isEmpty()) {
            return;
        }

        int safeLimit = safeLimit(limit, MIN_SOURCE_LIMIT);

        items.stream()
                .limit(safeLimit)
                .forEach(item -> {
                    if (item.getId() == null) {
                        return;
                    }

                    item.setItemType(itemType);

                    if (item.getScore() == null && score != null) {
                        item.setScore(score);
                    }

                    if (arm != null) {
                        item.setReasons(
                                List.of("BANDIT_" + arm.name()));

                        banditService.registerImpression(
                                userId,
                                itemType,
                                item.getId(),
                                arm);
                    }

                    result.putIfAbsent(
                            item.getId(),
                            item);
                });
    }

    private int calculateDynamicLimit(
            int availableSlots,
            Map<RecommendationArm, Double> weights,
            RecommendationArm arm) {
        double weight = weights.getOrDefault(arm, 0.25);

        return Math.max(
                MIN_SOURCE_LIMIT,
                (int) Math.round(availableSlots * weight));
    }

    private int safeLimit(
            Integer value,
            int defaultValue) {
        if (value == null || value <= 0) {
            return defaultValue;
        }

        return value;
    }

    private String extractPreferredDistrict(
            List<PropertyResponseDTO> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }

        Map<String, Integer> counter = new LinkedHashMap<>();

        for (PropertyResponseDTO item : items) {
            String district = item.getDistrict();

            if (district == null || district.isBlank()) {
                continue;
            }

            counter.put(
                    district,
                    counter.getOrDefault(district, 0) + 1);
        }

        return counter.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private String extractPreferredDistrictFromReels(
            List<PropertyReelResponseDTO> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }

        Map<String, Integer> counter = new LinkedHashMap<>();

        for (PropertyReelResponseDTO item : items) {
            String address = item.getAddress();

            if (address == null || address.isBlank()) {
                continue;
            }

            counter.put(
                    address,
                    counter.getOrDefault(address, 0) + 1);
        }

        return counter.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private List<PropertyReelResponseDTO> getResultList(
            ApiResponse<List<PropertyReelResponseDTO>> response) {
        if (response == null || response.getResult() == null) {
            return List.of();
        }

        return response.getResult();
    }

    private Double getOwnerTrustScore(
            Long ownerId) {
        if (ownerId == null) {
            return 0.0;
        }

        try {
            ApiResponse<Double> response = propertyClient.getOwnerTrustScore(ownerId);

            if (response == null || response.getResult() == null) {
                return 0.0;
            }

            return response.getResult();
        } catch (Exception e) {
            return 0.0;
        }
    }
}