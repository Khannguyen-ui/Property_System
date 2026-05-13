package com.homeverse.aiworker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ChatMemoryService {

    private final StringRedisTemplate redisTemplate;
    private static final int MAX_HISTORY = 5; // Chỉ nhớ 10 câu gần nhất cho đỡ tốn Token

    // Lưu tin nhắn vào Redis
    public void saveMessage(String conversationId, String role, String content) {
        String key = "chat_history:" + conversationId;
        String formattedMessage = role + ": " + content;

        redisTemplate.opsForList().rightPush(key, formattedMessage);


        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > MAX_HISTORY) {
            redisTemplate.opsForList().leftPop(key);
        }

        // Cài thời gian hết hạn là 1 ngày
        redisTemplate.expire(key, 24, TimeUnit.HOURS);
    }

    // Lấy lịch sử chat ra để mớm cho Gemini
    public String getChatHistory(String conversationId) {
        String key = "chat_history:" + conversationId;
        List<String> history = redisTemplate.opsForList().range(key, 0, -1);

        if (history == null || history.isEmpty()) {
            return "Chưa có lịch sử trò chuyện.";
        }
        return String.join("\n", history);
    }
}