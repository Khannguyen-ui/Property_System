package com.homeverse.recommendation.service;

import com.homeverse.recommendation.dto.BanditArmStats;
import com.homeverse.recommendation.enums.RecommendationArm;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BanditService {

    private final StringRedisTemplate redisTemplate;

    private static final double MIN_WEIGHT = 0.10;
    private static final double EXPLORATION = 0.25;

    public Map<RecommendationArm, Double> getWeights(Long userId) {
        Map<RecommendationArm, Double> scores = new EnumMap<>(RecommendationArm.class);

        double totalImpressions = getTotalImpressions(userId);

        for (RecommendationArm arm : RecommendationArm.values()) {
            double impressions = getImpressions(userId, arm);
            double rewards = getRewards(userId, arm);

            double avgReward = impressions > 0 ? rewards / impressions : 0.0;

            double explorationBoost = Math.sqrt(
                    Math.log(totalImpressions + 1.0) / (impressions + 1.0)
            );

            double score = avgReward + EXPLORATION * explorationBoost;

            scores.put(arm, Math.max(score, MIN_WEIGHT));
        }

        double totalScore = scores.values()
                .stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        Map<RecommendationArm, Double> weights = new EnumMap<>(RecommendationArm.class);

        for (Map.Entry<RecommendationArm, Double> entry : scores.entrySet()) {
            weights.put(
                    entry.getKey(),
                    entry.getValue() / totalScore
            );
        }

        return weights;
    }

    public void registerImpression(
            Long userId,
            String itemType,
            Long itemId,
            RecommendationArm arm
    ) {
        if (userId == null || itemType == null || itemId == null || arm == null) {
            return;
        }

        String armKey = getArmKey(userId, arm);
        String itemSourceKey = getItemSourceKey(userId, itemType, itemId);

        redisTemplate.opsForHash().increment(armKey, "impressions", 1.0);

        redisTemplate.opsForValue().set(
                itemSourceKey,
                arm.name(),
                24,
                TimeUnit.HOURS
        );

        redisTemplate.expire(armKey, 30, TimeUnit.DAYS);
    }

    public void recordReward(
            Long userId,
            String itemType,
            Long itemId,
            String action
    ) {
        if (userId == null || itemType == null || itemId == null || action == null) {
            return;
        }

        String itemSourceKey = getItemSourceKey(userId, itemType, itemId);
        String armName = redisTemplate.opsForValue().get(itemSourceKey);

        if (armName == null || armName.isBlank()) {
            return;
        }

        RecommendationArm arm;

        try {
            arm = RecommendationArm.valueOf(armName);
        } catch (Exception e) {
            return;
        }

        double reward = getReward(action);

        if (reward <= 0) {
            return;
        }

        String armKey = getArmKey(userId, arm);

        redisTemplate.opsForHash().increment(armKey, "rewards", reward);
        redisTemplate.expire(armKey, 30, TimeUnit.DAYS);
    }

    public List<BanditArmStats> getStats(Long userId) {
        Map<RecommendationArm, Double> weights = getWeights(userId);
        List<BanditArmStats> result = new ArrayList<>();

        for (RecommendationArm arm : RecommendationArm.values()) {
            double impressions = getImpressions(userId, arm);
            double rewards = getRewards(userId, arm);

            result.add(
                    BanditArmStats.builder()
                            .arm(arm.name())
                            .impressions(impressions)
                            .rewards(rewards)
                            .avgReward(impressions > 0 ? rewards / impressions : 0.0)
                            .weight(weights.getOrDefault(arm, 0.0))
                            .build()
            );
        }

        return result;
    }

    private double getReward(String action) {
        return switch (action.toUpperCase()) {
            case "VIEW" -> 0.1;
            case "CLICK" -> 0.3;
            case "LIKE" -> 0.7;
            case "SAVE" -> 0.9;
            case "CONTACT" -> 1.2;
            default -> 0.0;
        };
    }

    private double getTotalImpressions(Long userId) {
        double total = 0.0;

        for (RecommendationArm arm : RecommendationArm.values()) {
            total += getImpressions(userId, arm);
        }

        return total;
    }

    private double getImpressions(Long userId, RecommendationArm arm) {
        Object value = redisTemplate.opsForHash()
                .get(getArmKey(userId, arm), "impressions");

        return value != null ? Double.parseDouble(value.toString()) : 0.0;
    }

    private double getRewards(Long userId, RecommendationArm arm) {
        Object value = redisTemplate.opsForHash()
                .get(getArmKey(userId, arm), "rewards");

        return value != null ? Double.parseDouble(value.toString()) : 0.0;
    }

    private String getArmKey(Long userId, RecommendationArm arm) {
        return "bandit:user:" + userId + ":arm:" + arm.name();
    }

    private String getItemSourceKey(
            Long userId,
            String itemType,
            Long itemId
    ) {
        return "bandit:user:" + userId + ":item:" + itemType + ":" + itemId;
    }
}