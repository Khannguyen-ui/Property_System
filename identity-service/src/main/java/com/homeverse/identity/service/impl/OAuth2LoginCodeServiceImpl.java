package com.homeverse.identity.service.impl;

import com.homeverse.identity.service.OAuth2LoginCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuth2LoginCodeServiceImpl implements OAuth2LoginCodeService {

    private static final String PREFIX = "OAUTH2_CODE:";
    private static final Duration TTL = Duration.ofMinutes(2);

    private final StringRedisTemplate redisTemplate;

    @Override
    public String issueCode(String email) {
        String code = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(PREFIX + code, email, TTL);
        return code;
    }

    @Override
    public String consumeEmail(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        String key = PREFIX + code;
        String email = redisTemplate.opsForValue().get(key);
        if (email != null) {
            redisTemplate.delete(key);
        }
        return email;
    }
}