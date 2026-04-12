package com.homeverse.property.controller;

import com.homeverse.property.service.InteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/properties/{id}")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    @PostMapping("/like")
    public ResponseEntity<String> toggleLike(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @PathVariable Long id) {

        Long userId = extractUserId(authentication);
        validateIdentifier(userId, guestId);

        // Truyền đủ 3 tham số và nhận lại kết quả true/false
        boolean isLiked = interactionService.toggleLike(userId, guestId, id);
        return ResponseEntity.ok(isLiked ? "Thao tác Like thành công" : "Đã bỏ Like (Unlike) thành công");
    }

    @PostMapping("/save")
    public ResponseEntity<String> toggleSave(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @PathVariable Long id) {

        Long userId = extractUserId(authentication);
        validateIdentifier(userId, guestId);

        // Truyền đủ 3 tham số và nhận lại kết quả true/false
        boolean isSaved = interactionService.toggleSave(userId, guestId, id);
        return ResponseEntity.ok(isSaved ? "Đã lưu tin thành công" : "Đã bỏ lưu (Unsave) thành công");
    }

    // Hàm lấy ID cực kỳ an toàn, chống lỗi 500 Internal Server Error
    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null; // Trả về null để nó dùng guestId
        }
        try {
            return Long.valueOf(authentication.getPrincipal().toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Chặn đứng các request cố tình gọi API mà không có Token cũng không có Guest ID
    private void validateIdentifier(Long userId, String guestId) {
        if (userId == null && (guestId == null || guestId.trim().isEmpty())) {
            throw new IllegalArgumentException("Yêu cầu đăng nhập hoặc truyền X-Guest-Id qua Header");
        }
    }
}