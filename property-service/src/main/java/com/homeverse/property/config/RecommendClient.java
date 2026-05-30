package com.homeverse.property.config;

import com.homeverse.property.dto.request.TrackEventRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(
        name = "recommend-service",
        url = "${services.recommend-service.url:http://localhost:8092}"
)
public interface RecommendClient {

    @PostMapping("/recommend/track")
    Object track(TrackEventRequest request);
}