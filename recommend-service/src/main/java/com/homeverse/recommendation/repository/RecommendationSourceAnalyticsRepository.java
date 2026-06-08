package com.homeverse.recommendation.repository;

import com.homeverse.recommendation.model.RecommendationSourceAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendationSourceAnalyticsRepository
        extends JpaRepository<RecommendationSourceAnalytics, Long> {

    long countBySourceAndEventTypeIgnoreCase(
            String source,
            String eventType
    );

    long countByEventTypeIgnoreCase(
            String eventType
    );

    long countBySourceIgnoreCase(
            String source
    );
}