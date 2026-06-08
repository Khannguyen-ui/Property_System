package com.homeverse.property.config;

import com.homeverse.property.dto.request.OwnerRatingTrackRequest;
import com.homeverse.property.dto.request.TrackEventRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "recommend-service", url = "${homeverse.services.recommend-service.url}")
public interface RecommendClient {

    @PostMapping("/recommend/track")
    Object track(@RequestBody TrackEventRequest request);

    @PostMapping("/recommend/owner-rating/track")
    void trackOwnerRating(@RequestBody OwnerRatingTrackRequest request);
}