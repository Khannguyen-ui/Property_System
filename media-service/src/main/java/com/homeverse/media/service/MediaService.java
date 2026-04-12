package com.homeverse.media.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.EagerTransformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    private final Cloudinary cloudinary;

    // Hàm cũ của sếp (Upload 1 ảnh)
    public String uploadImage(MultipartFile file, String folderName) throws IOException {
        Map<String, Object> uploadParams = ObjectUtils.asMap(
                "folder", "homeverse/" + folderName
        );
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);
        return uploadResult.get("secure_url").toString();
    }

    // THÊM MỚI: Hàm upload nhiều ảnh cùng lúc (Dùng Đa luồng cho lẹ)
    public List<String> uploadImages(List<MultipartFile> files, String folderName) {
        log.info("Bắt đầu upload {} ảnh lên thư mục {}...", files.size(), folderName);

        // parallelStream() giúp đẩy 5 ảnh lên Cloudinary cùng 1 giây thay vì phải đợi từng cái
        return files.parallelStream().map(file -> {
            try {
                return uploadImage(file, folderName);
            } catch (IOException e) {
                log.error("Lỗi khi upload file {}: {}", file.getOriginalFilename(), e.getMessage());
                // Chỗ này sếp có thể throw AppException giống các service khác cũng được
                throw new RuntimeException("Không thể upload ảnh: " + file.getOriginalFilename());
            }
        }).collect(Collectors.toList());
    }
    // Thêm hàm này vào MediaService.java
    public String uploadVideo(MultipartFile file, String folderName) throws IOException {
        log.info("Đang xử lý upload Video lên Cloudinary...");

        Map<String, Object> uploadParams = ObjectUtils.asMap(
                "folder", "homeverse/" + folderName,
                "resource_type", "video", // QUAN TRỌNG: Phải có dòng này để Cloudinary biết là Video
                "eager", List.of(
                        // Yêu cầu Cloudinary tự động nén video cho nhẹ (Tối ưu cho Reels/Mobile)
                        new EagerTransformation().width(720).height(1280).crop("pad").fetchFormat("mp4")
                )
        );
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);
        return uploadResult.get("secure_url").toString();
    }
}