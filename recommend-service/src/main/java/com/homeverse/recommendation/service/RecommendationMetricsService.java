package com.homeverse.recommendation.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
@Getter
public class RecommendationMetricsService {

    private final Counter trackCounter;

    private final Counter spamBlockedCounter;

    private final Counter finalFeedCounter;

    private final Counter mlPredictCounter;

    private final Timer finalFeedTimer;

    private final Timer trackTimer;

    public RecommendationMetricsService(MeterRegistry meterRegistry) {
        this.trackCounter = Counter.builder("recommend_track_total")
                .description("Total recommendation tracking events")
                .register(meterRegistry);

        this.spamBlockedCounter = Counter.builder("recommend_spam_blocked_total")
                .description("Total blocked spam events")
                .register(meterRegistry);

        this.finalFeedCounter = Counter.builder("recommend_final_feed_total")
                .description("Total final feed requests")
                .register(meterRegistry);

        this.mlPredictCounter = Counter.builder("recommend_ml_predict_total")
                .description("Total ML predict requests")
                .register(meterRegistry);

        this.finalFeedTimer = Timer.builder("recommend_final_feed_latency")
                .description("Final feed latency")
                .register(meterRegistry);

        this.trackTimer = Timer.builder("recommend_track_latency")
                .description("Track endpoint latency")
                .register(meterRegistry);
    }
}