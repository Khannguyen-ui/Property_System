package com.homeverse.property.controller;

import com.homeverse.property.config.RecommendClient;
import com.homeverse.property.dto.request.TrackEventRequest;
import com.homeverse.property.dto.response.InteractionPropertyDTO;
import com.homeverse.property.service.InteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.homeverse.property.entity.Property;
import com.homeverse.property.repository.PropertyRepository;
@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;
    private final RecommendClient recommendClient;
    private final PropertyRepository propertyRepository;
    @PostMapping("/{id}/like")
    public ResponseEntity<String> toggleLike(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @PathVariable Long id) {

        Long userId = extractUserId(authentication);
        validateIdentifier(userId, guestId);

        boolean isLiked = interactionService.toggleLike(userId, guestId, id);

        if (isLiked && userId != null) {
            trackInteraction(userId, id, "LIKE");
        }

        return ResponseEntity.ok(isLiked ? "Thao tác Like thành công" : "Đã bỏ Like (Unlike) thành công");
    }

    @PostMapping("/{id}/save")
    public ResponseEntity<String> toggleSave(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @PathVariable Long id) {

        Long userId = extractUserId(authentication);
        validateIdentifier(userId, guestId);

        boolean isSaved = interactionService.toggleSave(userId, guestId, id);

        if (isSaved && userId != null) {
            trackInteraction(userId, id, "SAVE");
        }

        return ResponseEntity.ok(isSaved ? "Đã lưu tin thành công" : "Đã bỏ lưu (Unsave) thành công");
    }

    @GetMapping("/me/liked")
    public ResponseEntity<Page<InteractionPropertyDTO>> getMyLikedProperties(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long userId = extractUserId(authentication);
        validateIdentifier(userId, guestId);

        return ResponseEntity.ok(interactionService.getLikedProperties(userId, guestId, page, size));
    }

    @GetMapping("/me/saved")
    public ResponseEntity<Page<InteractionPropertyDTO>> getMySavedProperties(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long userId = extractUserId(authentication);
        validateIdentifier(userId, guestId);

        return ResponseEntity.ok(interactionService.getSavedProperties(userId, guestId, page, size));
    }

    private void trackInteraction(Long userId, Long propertyId, String action) {
    try {
        Property property = propertyRepository.findById(propertyId)
                .orElse(null);

        if (property == null) {
            return;
        }

        recommendClient.track(
                TrackEventRequest.builder()
                        .userId(userId)
                        .itemId(propertyId)
                        .itemType(
                                property.getVideoUrl() != null && !property.getVideoUrl().isBlank()
                                        ? "reel"
                                        : "property"
                        )
                        .action(action)
                        .watchTime(0.0)
                        .duration(1.0)
                        .price(
                                property.getPrice() != null
                                        ? property.getPrice().doubleValue()
                                        : 0.0
                        )
                        .userBudget(
                                property.getPrice() != null
                                        ? property.getPrice().doubleValue()
                                        : 0.0
                        )
                        .locationMatch(
                                property.getDistrict() != null && !property.getDistrict().isBlank()
                                        ? 1
                                        : 0
                        )
                        .categoryMatch(
                                property.getPropertyType() != null
                                        ? 1
                                        : 0
                        )
                        .district(property.getDistrict())
                        .build()
        );
    } catch (Exception e) {
        e.printStackTrace();
    }
}

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        try {
            return Long.valueOf(authentication.getPrincipal().toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void validateIdentifier(Long userId, String guestId) {
        if (userId == null && (guestId == null || guestId.trim().isEmpty())) {
            throw new IllegalArgumentException("Yêu cầu đăng nhập hoặc truyền X-Guest-Id qua Header");
        }
    }
}