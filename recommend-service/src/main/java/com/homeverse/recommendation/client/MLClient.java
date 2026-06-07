package com.homeverse.recommendation.client;

import com.homeverse.recommendation.dto.PredictRequest;
import com.homeverse.recommendation.dto.PredictResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "ml-client",
        url = "${homeverse.services.ml}"
)
public interface MLClient {

    @PostMapping("/ml/predict")
    PredictResponse predict(@RequestBody PredictRequest request);
}