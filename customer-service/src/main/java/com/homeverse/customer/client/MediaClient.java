package com.homeverse.customer.client;

import com.homeverse.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "media-service", url = "${MEDIA_SERVICE_URL}")
public interface MediaClient {


    @PostMapping(value = "/api/v1/media/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<String> uploadImage(@RequestPart("file") MultipartFile file, @RequestPart("folder") String folder);

    @DeleteMapping(value = "/api/v1/media/delete")
    void deleteImage(@RequestParam("fileUrl") String fileUrl);
}