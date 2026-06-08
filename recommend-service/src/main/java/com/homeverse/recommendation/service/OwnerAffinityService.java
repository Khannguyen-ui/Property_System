package com.homeverse.recommendation.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OwnerAffinityService {

    private final StringRedisTemplate redisTemplate;

    public void trackRating(Long userId, Long ownerId, Integer rating) {
        if (userId == null || ownerId == null || rating == null) {
            return;
        }

        double delta = calculateDelta(rating);

        String key = "user:" + userId + ":owner-affinity";

        redisTemplate.opsForZSet().incrementScore(
                key,
                ownerId.toString(),
                delta
        );
    }

    public double getAffinityScore(Long userId, Long ownerId) {
        if (userId == null || ownerId == null) {
            return 0.0;
        }

        String key = "user:" + userId + ":owner-affinity";

        Double score = redisTemplate.opsForZSet().score(
                key,
                ownerId.toString()
        );

        return score != null ? score : 0.0;
    }

    private double calculateDelta(Integer rating) {
        return switch (rating) {
            case 5 -> 1.0;
            case 4 -> 0.6;
            case 3 -> 0.0;
            case 2 -> -0.5;
            case 1 -> -1.0;
            default -> 0.0;
        };
    }
}
