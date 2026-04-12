package com.homeverse.identity.controller;

import com.homeverse.common.dto.ApiResponse;
import com.homeverse.identity.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // API Trừ lượt đăng (property-service gọi tới đây khi Admin duyệt bài)
    @PostMapping("/{userId}/use-post-quota")
    public ApiResponse<Void> usePostQuota(@PathVariable("userId") Long userId) {
        userService.usePostQuota(userId);

        return ApiResponse.<Void>builder()
                .message("Trừ lượt đăng thành công")
                .build();
    }

    // API Hoàn lượt đăng (property-service gọi tới đây khi xảy ra lỗi cần rollback)
    @PostMapping("/{userId}/refund-post-quota")
    public ApiResponse<Void> refundPostQuota(@PathVariable("userId") Long userId) {
        userService.refundPostQuota(userId);

        return ApiResponse.<Void>builder()
                .message("Hoàn lượt đăng thành công")
                .build();
    }
}