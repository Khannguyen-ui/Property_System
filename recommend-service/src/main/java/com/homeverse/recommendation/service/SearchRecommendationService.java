package com.homeverse.recommendation.service;

import com.homeverse.recommendation.dto.SearchSuggestionResponse;
import com.homeverse.recommendation.dto.SearchTrackRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SearchRecommendationService {

    private final StringRedisTemplate redisTemplate;

    private static final String GLOBAL_SEARCH_KEY = "search:keywords";
    private static final String USER_SEARCH_PREFIX = "user:";
    private static final String USER_SEARCH_SUFFIX = ":search:intents";

    public void track(SearchTrackRequest request) {
        if (request == null || request.getKeyword() == null || request.getKeyword().isBlank()) {
            return;
        }

        String keyword = normalizeKeyword(request.getKeyword());

        redisTemplate.opsForZSet().incrementScore(
                GLOBAL_SEARCH_KEY,
                keyword,
                1.0
        );

        if (request.getUserId() != null) {
            String userKey = USER_SEARCH_PREFIX + request.getUserId() + USER_SEARCH_SUFFIX;

            redisTemplate.opsForList().leftPush(userKey, keyword);
            redisTemplate.expire(userKey, 2, TimeUnit.HOURS);

            Long size = redisTemplate.opsForList().size(userKey);

            if (size != null && size > 30) {
                redisTemplate.opsForList().trim(userKey, 0, 29);
            }
        }
    }

    public SearchSuggestionResponse suggest(String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);

        Set<ZSetOperations.TypedTuple<String>> items =
                redisTemplate.opsForZSet().reverseRangeWithScores(GLOBAL_SEARCH_KEY, 0, 100);

        List<String> suggestions = new ArrayList<>();

        if (items != null) {
            for (ZSetOperations.TypedTuple<String> item : items) {
                String value = item.getValue();

                if (value == null) {
                    continue;
                }

                if (normalizedKeyword.isBlank() || value.contains(normalizedKeyword)) {
                    suggestions.add(value);
                }

                if (suggestions.size() >= 10) {
                    break;
                }
            }
        }

        return SearchSuggestionResponse.builder()
                .keyword(normalizedKeyword)
                .suggestions(suggestions)
                .build();
    }

    public List<String> getTopKeywords() {
        Set<String> items =
                redisTemplate.opsForZSet().reverseRange(GLOBAL_SEARCH_KEY, 0, 9);

        if (items == null || items.isEmpty()) {
            return List.of();
        }

        return new ArrayList<>(items);
    }

    public String getLatestUserSearchIntent(Long userId) {
        if (userId == null) {
            return null;
        }

        String userKey = USER_SEARCH_PREFIX + userId + USER_SEARCH_SUFFIX;

        List<String> items = redisTemplate.opsForList().range(userKey, 0, 0);

        if (items == null || items.isEmpty()) {
            return null;
        }

        return items.get(0);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return "";
        }

        String text = keyword.trim().toLowerCase();

        text = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        text = text.replaceAll("\\s+", " ");

        return text;
    }
}