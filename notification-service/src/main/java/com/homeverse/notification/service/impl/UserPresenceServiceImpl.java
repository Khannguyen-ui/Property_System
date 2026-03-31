package com.homeverse.notification.service.impl;

import com.homeverse.notification.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPresenceServiceImpl implements UserPresenceService {

    private final StringRedisTemplate redisTemplate;
    private static final String PRESENCE_KEY_PREFIX = "USER_ONLINE_SESSIONS:";


    private static final long SESSION_TTL_HOURS = 2;

    @Override
    public void setOnline(String userId, String sessionId) {
        String key = PRESENCE_KEY_PREFIX + userId;
        redisTemplate.opsForSet().add(key, sessionId);

        redisTemplate.expire(key, SESSION_TTL_HOURS, TimeUnit.HOURS);
        log.info("User {} online (Session: {})", userId, sessionId);
    }

    @Override
    public void setOffline(String userId, String sessionId) {
        String key = PRESENCE_KEY_PREFIX + userId;
        redisTemplate.opsForSet().remove(key, sessionId);


        Long size = redisTemplate.opsForSet().size(key);
        if (size == null || size == 0) {
            redisTemplate.delete(key);
            log.info("User {} is now completely offline", userId);
        } else {

            redisTemplate.expire(key, SESSION_TTL_HOURS, TimeUnit.HOURS);
            log.info("User {} closed one session, still has {} sessions active", userId, size);
        }
    }

    @Override
    public boolean isOnline(String userId) {

        Boolean hasKey = redisTemplate.hasKey(PRESENCE_KEY_PREFIX + userId);
        if (Boolean.FALSE.equals(hasKey)) return false;

        Long size = redisTemplate.opsForSet().size(PRESENCE_KEY_PREFIX + userId);
        return size != null && size > 0;
    }
}