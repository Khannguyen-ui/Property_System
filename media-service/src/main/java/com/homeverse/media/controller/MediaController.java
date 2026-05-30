package com.homeverse.media.controller;

import com.homeverse.common.dto.ApiResponse;
import com.homeverse.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload")
    public ApiResponse<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder) throws IOException {

        String contentType = file.getContentType();

        String mediaUrl;

        if (contentType != null && contentType.startsWith("video/")) {
            mediaUrl = mediaService.uploadVideo(file, folder);
        } else {
            mediaUrl = mediaService.uploadImage(file, folder);
        }

        return ApiResponse.<String>builder()
                .result(mediaUrl)
                .build();
    }

    @PostMapping("/upload-multiple")
    public ApiResponse<List<String>> uploadMultipleFiles(
            @RequestParam("files") List<MultipartFile> files, // Chú ý: "files" số nhiều
            @RequestParam(value = "folder", defaultValue = "properties") String folder) { // Đổi mặc định thành
                                                                                          // properties

        List<String> imageUrls = mediaService.uploadImages(files, folder);

        return ApiResponse.<List<String>>builder()
                .result(imageUrls)
                .build();
    }
}