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

        long views = userBehaviorRepository.countByActionIgnoreCase("VIEW");
        long clicks = userBehaviorRepository.countByActionIgnoreCase("CLICK");
        long likes = userBehaviorRepository.countByActionIgnoreCase("LIKE");
        long saves = userBehaviorRepository.countByActionIgnoreCase("SAVE");
        long contacts = userBehaviorRepository.countByActionIgnoreCase("CONTACT");

        double ctr = views == 0 ? 0 : ((double) clicks / views) * 100;
        double contactRate = clicks == 0 ? 0 : ((double) contacts / clicks) * 100;
        double saveRate = clicks == 0 ? 0 : ((double) saves / clicks) * 100;
        double likeRate = clicks == 0 ? 0 : ((double) likes / clicks) * 100;

        return AnalyticsDashboardResponse.builder()
                .totalBehaviors(userBehaviorRepository.count())
                .totalViews(userBehaviorRepository.countByActionIgnoreCase("VIEW"))
                .totalClicks(userBehaviorRepository.countByActionIgnoreCase("CLICK"))
                .totalLikes(userBehaviorRepository.countByActionIgnoreCase("LIKE"))
                .totalSaves(userBehaviorRepository.countByActionIgnoreCase("SAVE"))
                .totalContacts(userBehaviorRepository.countByActionIgnoreCase("CONTACT"))
                .ctr(ctr)
                .contactRate(contactRate)
                .saveRate(saveRate)
                .likeRate(likeRate)
                .avgScore(userBehaviorRepository.avgScore())
                .topItemType(userBehaviorRepository.findTopItemType())
                .topAction(userBehaviorRepository.findTopAction())
                .build();
    }
}