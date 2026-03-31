package com.homeverse.customer.client;

import com.homeverse.customer.dto.external.FptOcrResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "fpt-ai-client", url = "https://api.fpt.ai")
public interface FptAiClient {

    @PostMapping(value = "/vision/idr/vnm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    FptOcrResponse detectIdCard(
            @RequestHeader("api-key") String apiKey,
            @RequestPart("image") MultipartFile image
    );
}