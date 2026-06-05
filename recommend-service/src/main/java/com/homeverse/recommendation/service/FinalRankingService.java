package com.homeverse.recommendation.service;

import com.homeverse.recommendation.dto.PropertyReelResponseDTO;
import com.homeverse.recommendation.dto.PropertyResponseDTO;
import com.homeverse.recommendation.model.RankingConfig;
import com.homeverse.recommendation.model.UserInterestProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FinalRankingService {

    private final RankingConfigService rankingConfigService;

    public List<PropertyResponseDTO> rankProperties(
        List<PropertyResponseDTO> items,
        String preferredDistrict,
        UserInterestProfile profile
) {
    RankingConfig config =
            rankingConfigService.getConfig();

    return items.stream()
            .sorted((a, b) -> Double.compare(
                    calculatePropertyScore(
                            b,
                            preferredDistrict,
                            profile,
                            config
                    ),
                    calculatePropertyScore(
                            a,
                            preferredDistrict,
                            profile,
                            config
                    )
            ))
            .toList();
}

    public List<PropertyReelResponseDTO> rankReels(
        List<PropertyReelResponseDTO> items,
        String preferredDistrict,
        UserInterestProfile profile
) {
    RankingConfig config =
            rankingConfigService.getConfig();

    return items.stream()
            .sorted((a, b) -> Double.compare(
                    calculateReelScore(
                            b,
                            preferredDistrict,
                            profile,
                            config
                    ),
                    calculateReelScore(
                            a,
                            preferredDistrict,
                            profile,
                            config
                    )
            ))
            .toList();
}

    private double calculatePropertyScore(
            PropertyResponseDTO item,
            String preferredDistrict,
            UserInterestProfile profile,
            RankingConfig config) {
        double score = item.getScore() != null ? item.getScore() : 0.3;

       Set<String> reasons = item.getReasons() != null
        ? new LinkedHashSet<>(item.getReasons())
        : new LinkedHashSet<>();

        if (Boolean.TRUE.equals(item.getIsPromoted())) {
            score += config.getPromotedBoost();
            reasons.add("PROMOTED");
        }

        if (preferredDistrict != null
                && item.getDistrict() != null
                && item.getDistrict().equalsIgnoreCase(preferredDistrict)) {
            score += config.getDistrictBoost();
            reasons.add("DISTRICT_MATCH");
        }

        if (profile != null
                && "property".equalsIgnoreCase(profile.getFavoriteItemType())) {
            score += config.getFavoriteTypeBoost();
            reasons.add("FAVORITE_PROPERTY");
        }

        if (profile != null
                && profile.getAvgBudget() != null
                && profile.getAvgBudget() > 0
                && item.getPrice() != null) {
            double price = item.getPrice().doubleValue();
            double budget = profile.getAvgBudget();
            double diffRatio = Math.abs(price - budget) / budget;

            if (diffRatio <= 0.1) {
                score += 0.4;
                reasons.add("BUDGET_STRONG_MATCH");
            } else if (diffRatio <= 0.25) {
                score += 0.2;
                reasons.add("BUDGET_MATCH");
            }
        }

        double freshnessBoost = calculateFreshnessBoost(item.getCreatedAt(), config);

        if (freshnessBoost > 0) {
            score += freshnessBoost;
            reasons.add("FRESH_ITEM");
        }

        if (item.getOwnerTrustScore() != null
                && item.getOwnerTrustScore() > 0) {
            score += item.getOwnerTrustScore()
                    * config.getOwnerTrustWeight();

            reasons.add("TRUSTED_OWNER");
        }

        if (reasons.isEmpty()) {
            reasons.add("EXPLORE");
        }

        item.setReasons(new ArrayList<>(reasons));

        return score;
    }

    private double calculateReelScore(
            PropertyReelResponseDTO item,
            String preferredDistrict,
            UserInterestProfile profile,
            RankingConfig config) {
        double score = item.getScore() != null ? item.getScore() : 0.3;

       Set<String> reasons = item.getReasons() != null
        ? new LinkedHashSet<>(item.getReasons())
        : new LinkedHashSet<>();
        if (Boolean.TRUE.equals(item.getIsPromoted())) {
            score += config.getPromotedBoost();
            reasons.add("PROMOTED");
        }

        if (preferredDistrict != null
                && item.getAddress() != null
                && item.getAddress()
                        .toLowerCase()
                        .contains(preferredDistrict.toLowerCase())) {
            score += config.getDistrictBoost();
            reasons.add("LOCATION_MATCH");
        }

        if (profile != null
                && "reel".equalsIgnoreCase(profile.getFavoriteItemType())) {
            score += config.getFavoriteTypeBoost();
            reasons.add("FAVORITE_REEL");
        }

        if (profile != null
                && "LIKE".equalsIgnoreCase(profile.getFavoriteAction())) {
            score += config.getFavoriteActionBoost();
            reasons.add("USER_OFTEN_LIKES");
        }

        double freshnessBoost = calculateFreshnessBoost(item.getCreatedAt(), config);

        if (freshnessBoost > 0) {
            score += freshnessBoost;
            reasons.add("FRESH_ITEM");
        }

        if (reasons.isEmpty()) {
            reasons.add("EXPLORE");
        }

        item.setReasons(new ArrayList<>(reasons));

        return score;
    }

    private double calculateFreshnessBoost(
            LocalDateTime createdAt,
            RankingConfig config) {
        if (createdAt == null) {
            return 0.0;
        }

        long hours = Duration.between(createdAt, LocalDateTime.now()).toHours();

        if (hours <= 1) {
            return config.getFreshOneHourBoost();
        }

        if (hours <= 24) {
            return config.getFreshOneDayBoost();
        }

        if (hours <= 72) {
            return config.getFreshThreeDaysBoost();
        }

        return 0.0;
    }

    private static class DiversityFilter {

        private final Map<Long, Integer> ownerCount = new HashMap<>();
        private final Map<String, Integer> districtCount = new HashMap<>();

        public boolean allowProperty(PropertyResponseDTO item) {
            Long ownerId = item.getOwnerId();
            String district = item.getDistrict();

            if (ownerId != null) {
                int count = ownerCount.getOrDefault(ownerId, 0);

                if (count >= 10) {
                    return false;
                }

                ownerCount.put(ownerId, count + 1);
            }

            if (district != null) {
                int count = districtCount.getOrDefault(district, 0);

                if (count >= 20) {
                    return false;
                }

                districtCount.put(district, count + 1);
            }

            return true;
        }

        public boolean allowReel(PropertyReelResponseDTO item) {
            String ownerSlug = item.getOwnerSlug();
            String address = item.getAddress();

            if (ownerSlug != null) {
                Long key = (long) ownerSlug.hashCode();

                int count = ownerCount.getOrDefault(key, 0);

                if (count >= 2) {
                    return false;
                }

                ownerCount.put(key, count + 1);
            }

            if (address != null) {
                String key = address.toLowerCase();

                int count = districtCount.getOrDefault(key, 0);

                if (count >= 4) {
                    return false;
                }

                districtCount.put(key, count + 1);
            }

            return true;
        }
    }
}