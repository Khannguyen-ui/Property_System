package com.homeverse.property.controller;

import com.homeverse.property.config.RecommendClient;
import com.homeverse.property.dto.request.TrackEventRequest;
import com.homeverse.property.dto.response.InteractionPropertyDTO;
import com.homeverse.property.entity.Property;
import com.homeverse.property.repository.PropertyRepository;
import com.homeverse.property.service.InteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
            @PathVariable Long id,
            @RequestBody(required = false) TrackEventRequest trackRequest) {

        Long userId = extractUserId(authentication);
        validateIdentifier(userId, guestId);

        boolean isLiked = interactionService.toggleLike(userId, guestId, id);

        if (isLiked && userId != null) {
            trackInteraction(userId, id, "LIKE", trackRequest);
        }

        return ResponseEntity.ok(isLiked ? "Thao tác Like thành công" : "Đã bỏ Like (Unlike) thành công");
    }

    @PostMapping("/{id}/save")
    public ResponseEntity<String> toggleSave(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @PathVariable Long id,
            @RequestBody(required = false) TrackEventRequest trackRequest) {

        Long userId = extractUserId(authentication);
        validateIdentifier(userId, guestId);

        boolean isSaved = interactionService.toggleSave(userId, guestId, id);

        if (isSaved && userId != null) {
            trackInteraction(userId, id, "SAVE", trackRequest);
        }

        return ResponseEntity.ok(isSaved ? "Đã lưu tin thành công" : "Đã bỏ lưu (Unsave) thành công");
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<String> trackView(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @PathVariable Long id) {

        Long userId = extractUserId(authentication);
        interactionService.trackView(userId, guestId, id);

        return ResponseEntity.ok("View tracked");
    }

    @PostMapping("/{id}/click")
    public ResponseEntity<String> trackClick(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @PathVariable Long id,
            @RequestBody(required = false) TrackEventRequest trackRequest) {

        Long userId = extractUserId(authentication);
        validateIdentifier(userId, guestId);

        interactionService.trackClick(userId, guestId, id);

        if (userId != null) {
            trackInteraction(userId, id, "CLICK", trackRequest);
        }

        return ResponseEntity.ok("Click tracked");
    }

    @PostMapping("/{propertyId}/share")
    public ResponseEntity<Void> shareProperty(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @PathVariable Long propertyId,
            @RequestBody(required = false) TrackEventRequest trackRequest) {

        Long userId = extractUserId(authentication);
        validateIdentifier(userId, guestId);

        interactionService.shareProperty(userId, propertyId);

        if (userId != null) {
            trackInteraction(userId, propertyId, "SHARE", trackRequest);
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{propertyId}/contact")
    public ResponseEntity<Void> contactProperty(
            Authentication authentication,
            @PathVariable Long propertyId,
            @RequestBody(required = false) TrackEventRequest trackRequest) {

        Long userId = extractUserId(authentication);
        validateIdentifier(userId, null);

        interactionService.contactProperty(userId, propertyId);
        trackInteraction(userId, propertyId, "CONTACT", trackRequest);

        return ResponseEntity.ok().build();
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

    private void trackInteraction(
            Long userId,
            Long propertyId,
            String action,
            TrackEventRequest request) {

        try {
            Property property = propertyRepository.findById(propertyId).orElse(null);

            if (property == null) {
                return;
            }

            double watchTime = request != null && request.getWatchTime() != null
                    ? request.getWatchTime()
                    : 0.0;

            double duration = request != null && request.getDuration() != null
                    ? request.getDuration()
                    : 1.0;

            recommendClient.track(
                    TrackEventRequest.builder()
                            .userId(userId)
                            .itemId(propertyId)
                            .itemType("PROPERTY")
                            .action(action)
                            .watchTime(watchTime)
                            .duration(duration)
                            .price(property.getPrice() != null ? property.getPrice().doubleValue() : 0.0)
                            .userBudget(property.getPrice() != null ? property.getPrice().doubleValue() : 0.0)
                            .locationMatch(property.getDistrict() != null && !property.getDistrict().isBlank() ? 1 : 0)
                            .categoryMatch(property.getPropertyType() != null ? 1 : 0)
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