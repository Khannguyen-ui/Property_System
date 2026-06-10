package com.homeverse.recommendation.service;

import com.homeverse.recommendation.dto.RecommendUpdateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RecommendRealtimeService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyUser(Long userId, String itemType) {
        RecommendUpdateEvent event = RecommendUpdateEvent.builder()
                .userId(userId)
                .itemType(itemType)
                .reason("SCORE_UPDATED")
                .updatedAt(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend(
                "/topic/recommend/" + userId,
                event
        );
    }
}