package com.homeverse.recommendation.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_source_analytics")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationSourceAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long itemId;

    private String itemType; // property / reel

    private String source; // BEHAVIOR / TRENDING / RANDOM...

    private String eventType; // IMPRESSION / CLICK / CONTACT

    private LocalDateTime createdAt;
}