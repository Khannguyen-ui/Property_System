package com.homeverse.recommendation.service;

import com.homeverse.recommendation.dto.FraudCheckResult;
import com.homeverse.recommendation.enums.FraudDecision;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final StringRedisTemplate redisTemplate;

    public FraudCheckResult check(
            Long userId,
            Long itemId,
            String itemType,
            String action
    ) {
        if (userId == null || itemId == null || itemType == null || action == null) {
            return FraudCheckResult.builder()
                    .decision(FraudDecision.ALLOW)
                    .reason("VALIDATION_SKIPPED")
                    .currentCount(0L)
                    .build();
        }

        String normalizedAction = action.toUpperCase();

        FraudCheckResult repeatedResult = checkRepeatedSameItem(
                userId,
                itemId,
                itemType,
                normalizedAction
        );

        if (repeatedResult.getDecision() == FraudDecision.BLOCK) {
            return repeatedResult;
        }

        FraudCheckResult actionRateResult = checkActionRate(
                userId,
                normalizedAction
        );

        if (actionRateResult.getDecision() == FraudDecision.BLOCK) {
            return actionRateResult;
        }

        FraudCheckResult contactRateResult = checkContactSpam(
                userId,
                normalizedAction
        );

        if (contactRateResult.getDecision() == FraudDecision.BLOCK) {
            return contactRateResult;
        }

        return FraudCheckResult.builder()
                .decision(FraudDecision.ALLOW)
                .reason("OK")
                .currentCount(0L)
                .build();
    }

    public boolean isSuspiciousUser(Long userId) {
        if (userId == null) {
            return false;
        }

        String key = "fraud:user:" + userId + ":suspicious";

        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private FraudCheckResult checkRepeatedSameItem(
            Long userId,
            Long itemId,
            String itemType,
            String action
    ) {
        String key = "fraud:user:" + userId
                + ":item:" + itemType
                + ":" + itemId
                + ":action:" + action;

        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        }

        long limit = getRepeatedLimit(action);

        if (count != null && count > limit) {
            markSuspicious(userId, "REPEATED_SAME_ITEM_" + action);

            return FraudCheckResult.builder()
                    .decision(FraudDecision.BLOCK)
                    .reason("REPEATED_SAME_ITEM_" + action)
                    .currentCount(count)
                    .build();
        }

        return FraudCheckResult.builder()
                .decision(FraudDecision.ALLOW)
                .reason("OK")
                .currentCount(count)
                .build();
    }

    private FraudCheckResult checkActionRate(
            Long userId,
            String action
    ) {
        String key = "fraud:user:" + userId + ":action:" + action + ":rate";

        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        }

        long limit = getActionRateLimit(action);

        if (count != null && count > limit) {
            markSuspicious(userId, "ACTION_RATE_LIMIT_" + action);

            return FraudCheckResult.builder()
                    .decision(FraudDecision.BLOCK)
                    .reason("ACTION_RATE_LIMIT_" + action)
                    .currentCount(count)
                    .build();
        }

        return FraudCheckResult.builder()
                .decision(FraudDecision.ALLOW)
                .reason("OK")
                .currentCount(count)
                .build();
    }

    private FraudCheckResult checkContactSpam(
            Long userId,
            String action
    ) {
        if (!"CONTACT".equals(action)) {
            return FraudCheckResult.builder()
                    .decision(FraudDecision.ALLOW)
                    .reason("OK")
                    .currentCount(0L)
                    .build();
        }

        String key = "fraud:user:" + userId + ":contact:10m";

        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, 10, TimeUnit.MINUTES);
        }

        if (count != null && count > 10) {
            markSuspicious(userId, "CONTACT_SPAM");

            return FraudCheckResult.builder()
                    .decision(FraudDecision.BLOCK)
                    .reason("CONTACT_SPAM")
                    .currentCount(count)
                    .build();
        }

        return FraudCheckResult.builder()
                .decision(FraudDecision.ALLOW)
                .reason("OK")
                .currentCount(count)
                .build();
    }

    private long getRepeatedLimit(String action) {
        return switch (action) {
            case "VIEW" -> 20;
            case "CLICK" -> 10;
            case "LIKE" -> 3;
            case "SAVE" -> 3;
            case "CONTACT" -> 2;
            default -> 10;
        };
    }

    private long getActionRateLimit(String action) {
        return switch (action) {
            case "VIEW" -> 120;
            case "CLICK" -> 60;
            case "LIKE" -> 30;
            case "SAVE" -> 20;
            case "CONTACT" -> 15;
            default -> 60;
        };
    }

    private void markSuspicious(Long userId, String reason) {
        String key = "fraud:user:" + userId + ":suspicious";

        redisTemplate.opsForValue().set(
                key,
                reason,
                24,
                TimeUnit.HOURS
        );
    }
}