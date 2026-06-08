package com.homeverse.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatPresenceService {

    private final StringRedisTemplate redisTemplate;

    public void markOnline(Long userId) {
    }

    public void markOffline(Long userId) {
    }

    public boolean isOnline(Long userId) {
        return false;
    }

    public String getLastSeen(Long userId) {
        return null;
    }
}