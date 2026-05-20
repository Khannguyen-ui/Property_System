package com.homeverse.recommendation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SessionPreferenceService {

    private final StringRedisTemplate redisTemplate;

    public void trackDistrict(Long userId, String district) {
        if (userId == null || district == null || district.isBlank()) {
            return;
        }

        String key = "session:user:" + userId + ":districts";

        redisTemplate.opsForList().leftPush(key, district);

        redisTemplate.expire(key, 2, TimeUnit.HOURS);

        Long size = redisTemplate.opsForList().size(key);

        if (size != null && size > 30) {
            redisTemplate.opsForList().trim(key, 0, 29);
        }
    }

    public String getFavoriteDistrict(Long userId) {
        if (userId == null) {
            return null;
        }

        String key = "session:user:" + userId + ":districts";

        List<String> districts =
                redisTemplate.opsForList().range(key, 0, 30);

        if (districts == null || districts.isEmpty()) {
            return null;
        }

        return districts.stream()
                .reduce((first, second) -> second)
                .orElse(null);
    }
}