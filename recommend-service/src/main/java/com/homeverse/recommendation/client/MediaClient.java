package com.homeverse.recommendation.client;

import com.homeverse.recommendation.dto.MediaResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "media-client",
        url = "${homeverse.services.media}"
)
public interface MediaClient {

    @GetMapping("/api/v1/media")
    MediaResponse getMediaById(@PathVariable Long id);
}