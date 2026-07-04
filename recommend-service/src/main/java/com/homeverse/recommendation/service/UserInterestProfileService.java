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
        Double budget = userBehaviorRepository.avgBudgetByUserId(userId);

        String province = userBehaviorRepository.findFavoriteProvinceByUserId(userId);
        String ward = userBehaviorRepository.findFavoriteWardByUserId(userId);
        String street = userBehaviorRepository.findFavoriteStreetByUserId(userId);
        String propertyType = userBehaviorRepository.findFavoritePropertyTypeByUserId(userId);
        String transactionType = userBehaviorRepository.findFavoriteTransactionTypeByUserId(userId);
        Double preferredArea = userBehaviorRepository.avgAreaByUserId(userId);

        UserInterestProfile profile = userInterestProfileRepository
                .findById(userId)
                .orElse(UserInterestProfile.builder().userId(userId).build());

        profile.setFavoriteItemType(favoriteItemType);
        profile.setFavoriteAction(favoriteAction);
        profile.setAvgScore(avgScore);
        profile.setBudget(budget);
        profile.setProvince(province);
        profile.setWard(ward);
        profile.setStreet(street);
        profile.setPropertyType(propertyType);
        profile.setTransactionType(transactionType);
        profile.setPreferredArea(preferredArea);
        profile.setUpdatedAt(LocalDateTime.now());

        return userInterestProfileRepository.save(profile);
    }

    public UserInterestProfile getProfile(Long userId) {
        return userInterestProfileRepository
                .findById(userId)
                .orElseGet(() -> updateProfile(userId));
    }
}