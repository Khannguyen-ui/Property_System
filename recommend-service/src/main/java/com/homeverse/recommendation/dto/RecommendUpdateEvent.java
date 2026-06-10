package com.homeverse.recommendation.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendUpdateEvent {
    private Long userId;
    private String itemType; // PROPERTY, REEL
    private String reason;   // SCORE_UPDATED
    private LocalDateTime updatedAt;
}