package com.homeverse.recommendation.service;

import com.homeverse.recommendation.model.UserInterestProfile;
import com.homeverse.recommendation.repository.UserBehaviorRepository;
import com.homeverse.recommendation.repository.UserInterestProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserInterestProfileService {

    private final UserBehaviorRepository userBehaviorRepository;
    private final UserInterestProfileRepository userInterestProfileRepository;

    public UserInterestProfile updateProfile(Long userId) {
        String favoriteItemType = userBehaviorRepository.findFavoriteItemTypeByUserId(userId);
        String favoriteAction = userBehaviorRepository.findFavoriteActionByUserId(userId);
        Double avgScore = userBehaviorRepository.avgScoreByUserId(userId);
        Double avgBudget = userBehaviorRepository.avgBudgetByUserId(userId);

        Double budget = userBehaviorRepository.avgBudgetByUserId(userId);

UserInterestProfile profile = UserInterestProfile.builder()
        .userId(userId)
        .favoriteItemType(favoriteItemType)
        .favoriteAction(favoriteAction)
        .avgScore(avgScore)
        .budget(budget)
        .updatedAt(LocalDateTime.now())
        .build();

        return userInterestProfileRepository.save(profile);
    }

    public UserInterestProfile getProfile(Long userId) {
        return userInterestProfileRepository
                .findById(userId)
                .orElseGet(() -> updateProfile(userId));
    }
}