package com.homeverse.recommendation.service;

import com.homeverse.common.dto.ApiResponse;
import com.homeverse.recommendation.client.PropertyClient;
import com.homeverse.recommendation.dto.CollaborativeItemProjection;
import com.homeverse.recommendation.dto.PropertyReelResponseDTO;
import com.homeverse.recommendation.dto.PropertyResponseDTO;
import com.homeverse.recommendation.repository.UserBehaviorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CollaborativeRecommendationService {

    private final UserBehaviorRepository userBehaviorRepository;
    private final PropertyClient propertyClient;

    public List<PropertyResponseDTO> getCollaborativeProperties(Long userId) {
        List<CollaborativeItemProjection> items =
                userBehaviorRepository.findCollaborativeProperties(userId);

        List<PropertyResponseDTO> result = new ArrayList<>();

        for (CollaborativeItemProjection item : items) {
            try {
                PropertyResponseDTO property =
                        propertyClient.getPropertyById(item.getItemId());

                property.setItemType("property");
                property.setScore(item.getCfScore());
                property.setReasons(List.of("SIMILAR_USERS_LIKED"));

                result.add(property);
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    public List<PropertyReelResponseDTO> getCollaborativeReels(Long userId) {
        List<CollaborativeItemProjection> items =
                userBehaviorRepository.findCollaborativeReels(userId);

        List<PropertyReelResponseDTO> result = new ArrayList<>();

        for (CollaborativeItemProjection item : items) {
            try {
                ApiResponse<PropertyReelResponseDTO> response =
                        propertyClient.getReelById(item.getItemId());

                if (response == null || response.getResult() == null) {
                    continue;
                }

                PropertyReelResponseDTO reel = response.getResult();

                reel.setItemType("reel");
                reel.setScore(item.getCfScore());
                reel.setReasons(List.of("SIMILAR_USERS_LIKED"));

                result.add(reel);
            } catch (Exception ignored) {
            }
        }

        return result;
    }
}