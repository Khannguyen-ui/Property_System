package com.homeverse.recommendation.controller;

import com.homeverse.recommendation.dto.FraudAnalyticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recommend/fraud")
@RequiredArgsConstructor
public class FraudController {

    private final StringRedisTemplate redisTemplate;

    @GetMapping("/users/{userId}")
    public FraudAnalyticsResponse getUserFraudStatus(
            @PathVariable Long userId
    ) {
        String suspiciousKey = "fraud:user:" + userId + ":suspicious";
        String reason = redisTemplate.opsForValue().get(suspiciousKey);

        return FraudAnalyticsResponse.builder()
                .blockedSpamLastMinute(0L)
                .suspicious(reason != null)
                .suspiciousReason(reason)
                .build();
    }
}