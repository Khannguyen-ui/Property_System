package com.homeverse.chat.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatPresenceServiceImpl {

    private final StringRedisTemplate redisTemplate;

    public void markOnline(Long userId) {
        System.out.println("ONLINE USER = " + userId);

        redisTemplate.opsForValue().set(
                "chat:user:" + userId + ":online",
                "true"
        );

        redisTemplate.opsForValue().set(
                "chat:user:" + userId + ":last_seen",
                LocalDateTime.now().toString()
        );
    }

    public void markOffline(Long userId) {
        System.out.println("OFFLINE USER = " + userId);

        redisTemplate.opsForValue().set(
                "chat:user:" + userId + ":online",
                "false"
        );

        redisTemplate.opsForValue().set(
                "chat:user:" + userId + ":last_seen",
                LocalDateTime.now().toString()
        );
    }

    public boolean isOnline(Long userId) {
        return "true".equals(
                redisTemplate.opsForValue().get("chat:user:" + userId + ":online")
        );
    }

    public String getLastSeen(Long userId) {
        return redisTemplate.opsForValue().get("chat:user:" + userId + ":last_seen");
    }
}