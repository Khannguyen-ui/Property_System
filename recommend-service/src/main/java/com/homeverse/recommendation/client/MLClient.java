package com.homeverse.recommendation.client;

import com.homeverse.recommendation.dto.PredictRequest;
import com.homeverse.recommendation.dto.PredictResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(
        name = "ml-client",
        url = "${ml.service.url}"
)
public interface MLClient {

    @PostMapping("/ml/predict")
    PredictResponse predict(PredictRequest request);
}