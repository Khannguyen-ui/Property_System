package com.homeverse.property.controller;

import com.homeverse.property.dto.request.TrackEventRequest;
import com.homeverse.property.dto.response.InteractionPropertyDTO;
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

    @PostMapping("/{id}/like")
    public ResponseEntity<String> toggleLike(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @PathVariable Long id) {

        Long userId = extractUserId(authentication);
        validateIdentifier(userId, guestId);

        boolean isLiked = interactionService.toggleLike(userId, guestId, id);

        return ResponseEntity.ok(
                isLiked
                        ? "Thao tác Like thành công"
                        : "Đã bỏ Like (Unlike) thành công"
        );
    }

    @PostMapping("/{id}/save")
    public ResponseEntity<String> toggleSave(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @PathVariable Long id) {

        Long userId = extractUserId(authentication);
        validateIdentifier(userId, guestId);

        boolean isSaved = interactionService.toggleSave(userId, guestId, id);

        return ResponseEntity.ok(
                isSaved
                        ? "Đã lưu tin thành công"
                        : "Đã bỏ lưu (Unsave) thành công"
        );
    }

    @PostMapping("/{id}/view")
public ResponseEntity<String> trackView(
        Authentication authentication,
        @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
        @PathVariable Long id,
        @RequestBody(required = false) TrackEventRequest trackRequest) {

    Long userId = extractUserId(authentication);

    double watchTime = trackRequest != null && trackRequest.getWatchTime() != null
            ? trackRequest.getWatchTime()
            : 0.0;

    double duration = trackRequest != null && trackRequest.getDuration() != null
            ? trackRequest.getDuration()
            : 1.0;

    interactionService.trackView(userId, guestId, id, watchTime, duration);

    return ResponseEntity.ok("View tracked");
}

    @PostMapping("/{propertyId}/share")
    public ResponseEntity<Void> shareProperty(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @PathVariable Long propertyId) {

        Long userId = extractUserId(authentication);
        validateIdentifier(userId, guestId);

        interactionService.shareProperty(userId, propertyId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{propertyId}/contact")
    public ResponseEntity<Void> contactProperty(
            Authentication authentication,
            @PathVariable Long propertyId) {

        Long userId = extractUserId(authentication);
        validateIdentifier(userId, null);

        interactionService.contactProperty(userId, propertyId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{propertyId}/click")
    public ResponseEntity<String> trackClick(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @PathVariable Long propertyId) {

        Long userId = extractUserId(authentication);
        validateIdentifier(userId, guestId);

        interactionService.trackClick(userId, guestId, propertyId);

        return ResponseEntity.ok("Click tracked");
    }

    @GetMapping("/me/liked")
    public ResponseEntity<Page<InteractionPropertyDTO>> getMyLikedProperties(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long userId = extractUserId(authentication);
        validateIdentifier(userId, guestId);

        return ResponseEntity.ok(
                interactionService.getLikedProperties(userId, guestId, page, size)
        );
    }

    @GetMapping("/me/saved")
    public ResponseEntity<Page<InteractionPropertyDTO>> getMySavedProperties(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long userId = extractUserId(authentication);
        validateIdentifier(userId, guestId);

        return ResponseEntity.ok(
                interactionService.getSavedProperties(userId, guestId, page, size)
        );
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