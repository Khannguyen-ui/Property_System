package com.homeverse.aiworker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeverse.aiworker.dto.internal.LastSearchContextDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMemoryService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final int MAX_HISTORY = 10;

    private static final String CHAT_HISTORY_PREFIX = "chat_history:";
    private static final String LAST_SEARCH_PREFIX = "chat_last_search:";

    public void saveMessage(String userId, String conversationId, String role, String content) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }

        String key = buildChatHistoryKey(userId, conversationId);
        String safeContent = content == null ? "" : content;
        String formattedMessage = role + ": " + safeContent;

        redisTemplate.opsForList().rightPush(key, formattedMessage);

        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > MAX_HISTORY) {
            redisTemplate.opsForList().leftPop(key);
        }

        redisTemplate.expire(key, 24, TimeUnit.HOURS);
    }

    public String getChatHistory(String userId, String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return "Chưa có lịch sử trò chuyện.";
        }

        String key = buildChatHistoryKey(userId, conversationId);
        List<String> history = redisTemplate.opsForList().range(key, 0, -1);

        if (history == null || history.isEmpty()) {
            return "Chưa có lịch sử trò chuyện.";
        }

        return String.join("\n", history);
    }

    public void saveLastSearchContext(
            String userId,
            String conversationId,
            String userQuery,
            List<Long> propertyIds,
            Integer totalMatched,
            Boolean hasMore
    ) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }

        try {
            LastSearchContextDTO context = LastSearchContextDTO.builder()
                    .userId(safeKey(userId))
                    .conversationId(conversationId)
                    .lastUserQuery(userQuery == null ? "" : userQuery)
                    .lastPropertyIds(propertyIds == null ? List.of() : propertyIds)
                    .totalMatched(totalMatched == null ? 0 : totalMatched)
                    .hasMore(Boolean.TRUE.equals(hasMore))
                    .build();

            String key = buildLastSearchKey(userId, conversationId);
            String json = objectMapper.writeValueAsString(context);

            redisTemplate.opsForValue().set(key, json, Duration.ofHours(24));

        } catch (Exception e) {
            log.warn("Không lưu được last search context: userId={}, conversationId={}",
                    userId, conversationId, e);
        }
    }

    public LastSearchContextDTO getLastSearchContext(String userId, String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return null;
        }

        try {
            String key = buildLastSearchKey(userId, conversationId);
            String json = redisTemplate.opsForValue().get(key);

            if (json == null || json.isBlank()) {
                return null;
            }

            return objectMapper.readValue(json, LastSearchContextDTO.class);

        } catch (Exception e) {
            log.warn("Không đọc được last search context: userId={}, conversationId={}",
                    userId, conversationId, e);
            return null;
        }
    }

    public void clearLastSearchContext(String userId, String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }

        redisTemplate.delete(buildLastSearchKey(userId, conversationId));
    }

    private String buildChatHistoryKey(String userId, String conversationId) {
        return CHAT_HISTORY_PREFIX + safeKey(userId) + ":" + safeKey(conversationId);
    }

    private String buildLastSearchKey(String userId, String conversationId) {
        return LAST_SEARCH_PREFIX + safeKey(userId) + ":" + safeKey(conversationId);
    }

    private String safeKey(String value) {
        return value == null || value.isBlank()
                ? "anonymous"
                : value.trim().replaceAll("\\s+", "_");
    }
}