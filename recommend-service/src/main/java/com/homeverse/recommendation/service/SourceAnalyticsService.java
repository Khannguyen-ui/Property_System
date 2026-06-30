package com.homeverse.recommendation.service;

import com.homeverse.recommendation.dto.SourceAnalyticsTrackRequest;
import com.homeverse.recommendation.dto.SourceCTRResponse;
import com.homeverse.recommendation.model.RecommendationSourceAnalytics;
import com.homeverse.recommendation.repository.RecommendationSourceAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SourceAnalyticsService {

    private final RecommendationSourceAnalyticsRepository repository;

    public void track(SourceAnalyticsTrackRequest request) {
        repository.save(
                RecommendationSourceAnalytics.builder()
                        .userId(request.getUserId())
                        .itemId(request.getItemId())
                        .itemType(request.getItemType())
                        .source(request.getSource())
                        .eventType(request.getEventType())
                        .createdAt(LocalDateTime.now())
                        .build());
    }

    public SourceCTRResponse getCtrBySource(String source) {
        long impressions = repository.countBySourceAndEventTypeIgnoreCase(source, "IMPRESSION");
        long clicks = repository.countBySourceAndEventTypeIgnoreCase(source, "CLICK");
        long contacts = repository.countBySourceAndEventTypeIgnoreCase(source, "CONTACT");

        double ctr = impressions == 0 ? 0 : ((double) clicks / impressions) * 100;
        double contactRate = clicks == 0 ? 0 : ((double) contacts / clicks) * 100;

        return SourceCTRResponse.builder()
                .source(source)
                .impressions(impressions)
                .clicks(clicks)
                .contacts(contacts)
                .ctr(ctr)
                .contactRate(contactRate)
                .build();
    }

    public List<SourceCTRResponse> getAllSourcesCtr() {
        return List.of(
                getCtrBySource("BEHAVIOR"),
                getCtrBySource("COLLABORATIVE"),
                getCtrBySource("TRENDING"),
                getCtrBySource("RANDOM"),
                getCtrBySource("PROMOTED"),
                getCtrBySource("FOLLOWING_OWNER"),
                getCtrBySource("LIKED_OWNER"));
    }

    public String detectPrimarySource(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return "RANDOM";
        }

        if (reasons.contains("FOLLOWING_OWNER"))
            return "FOLLOWING_OWNER";
        if (reasons.contains("LIKED_OWNER"))
            return "LIKED_OWNER";
        if (reasons.contains("BANDIT_BEHAVIOR"))
            return "BEHAVIOR";
        if (reasons.contains("BANDIT_COLLABORATIVE"))
            return "COLLABORATIVE";
        if (reasons.contains("BANDIT_TRENDING") || reasons.contains("TRENDING"))
            return "TRENDING";
        if (reasons.contains("PROMOTED"))
            return "PROMOTED";
        if (reasons.contains("BANDIT_RANDOM") || reasons.contains("EXPLORE"))
            return "RANDOM";

        return reasons.get(0);
    }

    public void trackImpression(
            Long userId,
            Long itemId,
            String itemType,
            String source) {
        SourceAnalyticsTrackRequest request = new SourceAnalyticsTrackRequest();
        request.setUserId(userId);
        request.setItemId(itemId);
        request.setItemType(itemType);
        request.setSource(source);
        request.setEventType("IMPRESSION");

        track(request);
    }

    public SourceCTRResponse getBestSource() {
        return getAllSourcesCtr()
                .stream()
                .filter(s -> s.getImpressions() != null && s.getImpressions() > 0)
                .max((a, b) -> Double.compare(a.getCtr(), b.getCtr()))
                .orElse(null);
    }

    public SourceCTRResponse getWorstSource() {
        return getAllSourcesCtr()
                .stream()
                .filter(s -> s.getImpressions() != null && s.getImpressions() > 0)
                .min((a, b) -> Double.compare(a.getCtr(), b.getCtr()))
                .orElse(null);
    }
    public void trackClick(
        Long userId,
        Long itemId,
        String itemType,
        String source) {
    SourceAnalyticsTrackRequest request = new SourceAnalyticsTrackRequest();
    request.setUserId(userId);
    request.setItemId(itemId);
    request.setItemType(itemType);
    request.setSource(source);
    request.setEventType("CLICK");

    track(request);
}

public void trackContact(
        Long userId,
        Long itemId,
        String itemType,
        String source) {
    SourceAnalyticsTrackRequest request = new SourceAnalyticsTrackRequest();
    request.setUserId(userId);
    request.setItemId(itemId);
    request.setItemType(itemType);
    request.setSource(source);
    request.setEventType("CONTACT");

    track(request);
}
public String findLatestImpressionSource(Long userId, Long itemId, String itemType) {
    return repository
            .findTopByUserIdAndItemIdAndItemTypeAndEventTypeIgnoreCaseOrderByCreatedAtDesc(
                    userId,
                    itemId,
                    itemType,
                    "IMPRESSION"
            )
            .map(RecommendationSourceAnalytics::getSource)
            .orElse("RANDOM");
}
public void trackClick(Long userId, Long itemId, String itemType) {
    String source = findLatestImpressionSource(userId, itemId, itemType);

    SourceAnalyticsTrackRequest request = new SourceAnalyticsTrackRequest();
    request.setUserId(userId);
    request.setItemId(itemId);
    request.setItemType(itemType);
    request.setSource(source);
    request.setEventType("CLICK");

    track(request);
}
}