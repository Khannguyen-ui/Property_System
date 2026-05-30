package com.homeverse.recommendation.service;

import com.homeverse.recommendation.dto.AnalyticsDashboardResponse;
import com.homeverse.recommendation.repository.UserBehaviorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UserBehaviorRepository userBehaviorRepository;

    public AnalyticsDashboardResponse getDashboard() {
        return AnalyticsDashboardResponse.builder()
                .totalBehaviors(userBehaviorRepository.count())
                .totalViews(userBehaviorRepository.countByActionIgnoreCase("VIEW"))
                .totalClicks(userBehaviorRepository.countByActionIgnoreCase("CLICK"))
                .totalLikes(userBehaviorRepository.countByActionIgnoreCase("LIKE"))
                .totalSaves(userBehaviorRepository.countByActionIgnoreCase("SAVE"))
                .totalContacts(userBehaviorRepository.countByActionIgnoreCase("CONTACT"))
                .avgScore(userBehaviorRepository.avgScore())
                .topItemType(userBehaviorRepository.findTopItemType())
                .topAction(userBehaviorRepository.findTopAction())
                .build();
    }
}