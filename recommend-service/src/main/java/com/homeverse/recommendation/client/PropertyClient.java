package com.homeverse.recommendation.client;

import com.homeverse.common.dto.ApiResponse;
import com.homeverse.recommendation.dto.OwnerRatingSummaryResponse;
import com.homeverse.recommendation.dto.PropertyReelResponseDTO;
import com.homeverse.recommendation.dto.PropertyResponseDTO;
import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
@FeignClient(name = "property-client", url = "${homeverse.services.property}")
public interface PropertyClient {
    @GetMapping("/public/properties/all")
    List<PropertyResponseDTO> getAllActiveProperties();

    @GetMapping("/public/properties/{id}")
    PropertyResponseDTO getPropertyById(@PathVariable Long id);

    @GetMapping("/public/properties/reels/{id}")
    ApiResponse<PropertyReelResponseDTO> getReelById(@PathVariable Long id);

    @GetMapping("/public/properties/promoted")
    List<PropertyResponseDTO> getPromotedProperties();

    @GetMapping("/public/properties/trending")
    List<PropertyResponseDTO> getTrendingProperties();

    @GetMapping("/public/properties/random")
    List<PropertyResponseDTO> getRandomProperties();

    @GetMapping("/public/properties/reels/promoted")
    ApiResponse<List<PropertyReelResponseDTO>> getPromotedReels();

    @GetMapping("/public/properties/reels/trending")
    ApiResponse<List<PropertyReelResponseDTO>> getTrendingReels();

    @GetMapping("/public/properties/reels/random")
    ApiResponse<List<PropertyReelResponseDTO>> getRandomReels();

    @GetMapping("/public/properties/owners/{ownerId}/trust-score")
    ApiResponse<Double> getOwnerTrustScore(@PathVariable Long ownerId);

    @GetMapping("/owners/following/{followerId}")
    List<Long> getFollowedOwnerIds(@PathVariable Long followerId);

    @GetMapping("/owners/reviews/{ownerId}/summary")
    OwnerRatingSummaryResponse getOwnerRatingSummary(@PathVariable Long ownerId);
}