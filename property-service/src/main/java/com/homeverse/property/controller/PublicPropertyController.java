package com.homeverse.property.controller;

import com.homeverse.common.dto.ApiResponse;
import com.homeverse.property.dto.response.PropertyReelResponseDTO;
import com.homeverse.property.dto.response.PropertyResponseDTO;
import com.homeverse.property.dto.response.ReelsFeedResponse;
import com.homeverse.property.service.PropertyService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/public/properties")
@RequiredArgsConstructor
public class PublicPropertyController {

    private final PropertyService propertyService;

    // 1. Xem danh sách bài đăng (Dành cho Trang chủ)
    @GetMapping
    public ResponseEntity<Page<PropertyResponseDTO>> getAllProperties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(propertyService.getPublicProperties(page, size));
    }

    @GetMapping("/owners/{ownerId}/trust-score")
    public ApiResponse<Double> getOwnerTrustScore(@PathVariable Long ownerId) {
        return ApiResponse.<Double>builder()
                .result(propertyService.getOwnerTrustScore(ownerId))
                .build();
    }

    @GetMapping("/reels/{id}")
    public ApiResponse<PropertyReelResponseDTO> getReelById(@PathVariable Long id) {
        return ApiResponse.<PropertyReelResponseDTO>builder()
                .result(propertyService.getPropertyReelById(id))
                .build();
    }

    @GetMapping("/promoted")
    public ResponseEntity<List<PropertyResponseDTO>> getPromotedProperties() {
        return ResponseEntity.ok(
                propertyService.getPromotedProperties());
    }

    @GetMapping("/trending")
    public ResponseEntity<List<PropertyResponseDTO>> getTrendingProperties() {
        return ResponseEntity.ok(
                propertyService.getTrendingProperties());
    }

    @GetMapping("/random")
    public ResponseEntity<List<PropertyResponseDTO>> getRandomProperties() {
        return ResponseEntity.ok(
                propertyService.getRandomProperties());
    }

    @GetMapping("/reels/promoted")
    public ApiResponse<List<PropertyReelResponseDTO>> getPromotedReels() {
        return ApiResponse.<List<PropertyReelResponseDTO>>builder()
                .result(propertyService.getPromotedReels())
                .build();
    }

    @GetMapping("/reels/trending")
    public ApiResponse<List<PropertyReelResponseDTO>> getTrendingReels() {
        return ApiResponse.<List<PropertyReelResponseDTO>>builder()
                .result(propertyService.getTrendingReels())
                .build();
    }

    @GetMapping("/reels/random")
    public ApiResponse<List<PropertyReelResponseDTO>> getRandomReels() {
        return ApiResponse.<List<PropertyReelResponseDTO>>builder()
                .result(propertyService.getRandomReels())
                .build();
    }

    // 2. Xem chi tiết 1 bài đăng (Khi khách click vào Card)
    @GetMapping("/{id}")
    public ResponseEntity<PropertyResponseDTO> getPropertyDetail(@PathVariable Long id) {

        return ResponseEntity.ok(propertyService.getPublicPropertyDetail(id));
    }

    // 3. API lướt Reels (Video ngắn - Không cần đăng nhập)
    @GetMapping("/reels")
    public ApiResponse<ReelsFeedResponse> getReelsFeed(
            Authentication authentication, // Thêm Authentication để lấy thông tin người dùng đang gọi API
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {

        Long currentUserId = null;

        // Kiểm tra xem request này có kèm Token hợp lệ không
        if (authentication != null && authentication.getPrincipal() != null) {
            try {
                // Ép kiểu Principal thành String rồi parse sang Long (tùy thuộc vào cách bạn
                // setup JwtFilter)
                currentUserId = Long.valueOf(authentication.getPrincipal().toString());
            } catch (NumberFormatException e) {
                // Nếu parse lỗi thì kệ, coi như user chưa đăng nhập (public)
                currentUserId = null;
            }
        }

        // Truyền đủ 3 tham số: currentUserId, cursor, size
        return ApiResponse.<ReelsFeedResponse>builder()
                .result(propertyService.getReelsFeed(currentUserId, guestId, cursor, size))
                .build();
    }

    @GetMapping("/owners/{ownerId}")
    public ApiResponse<Page<PropertyResponseDTO>> getOwnerPublicProperties(
            @PathVariable Long ownerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ApiResponse.<Page<PropertyResponseDTO>>builder()
                .result(propertyService.getPropertiesByOwnerId(ownerId, page, size))
                .build();
    }
}