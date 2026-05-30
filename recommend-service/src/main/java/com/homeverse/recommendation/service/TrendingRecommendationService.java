package com.homeverse.recommendation.service;

import com.homeverse.common.dto.ApiResponse;
import com.homeverse.recommendation.client.PropertyClient;
import com.homeverse.recommendation.dto.PropertyReelResponseDTO;
import com.homeverse.recommendation.dto.PropertyResponseDTO;
import com.homeverse.recommendation.dto.TrendingItemProjection;
import com.homeverse.recommendation.repository.UserBehaviorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrendingRecommendationService {

    private final UserBehaviorRepository userBehaviorRepository;
    private final PropertyClient propertyClient;

    public List<PropertyResponseDTO> getTrendingProperties() {
        List<TrendingItemProjection> items =
                userBehaviorRepository.findTrendingProperties();

        List<PropertyResponseDTO> result = new ArrayList<>();

        for (TrendingItemProjection item : items) {
            try {
                PropertyResponseDTO property =
                        propertyClient.getPropertyById(item.getItemId());

                property.setItemType("property");
                property.setScore(item.getTrendingScore());

                result.add(property);
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    public List<PropertyReelResponseDTO> getTrendingReels() {
        List<TrendingItemProjection> items =
                userBehaviorRepository.findTrendingReels();

        List<PropertyReelResponseDTO> result = new ArrayList<>();

        for (TrendingItemProjection item : items) {
            try {
                ApiResponse<PropertyReelResponseDTO> response =
                        propertyClient.getReelById(item.getItemId());

                if (response == null || response.getResult() == null) {
                    continue;
                }

                PropertyReelResponseDTO reel = response.getResult();

                reel.setItemType("reel");
                reel.setScore(item.getTrendingScore());

                result.add(reel);
            } catch (Exception ignored) {
            }
        }

        return result;
    }
}