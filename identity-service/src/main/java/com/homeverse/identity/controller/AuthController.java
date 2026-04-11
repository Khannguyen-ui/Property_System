package com.homeverse.identity.controller;

import com.homeverse.common.dto.ApiResponse;
import com.homeverse.common.exception.AppException;
import com.homeverse.common.exception.ErrorCode;
import com.homeverse.identity.dto.request.*;
import com.homeverse.identity.dto.response.AuthResponse;
import com.homeverse.identity.dto.response.UserResponseDTO;
import com.homeverse.identity.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<UserResponseDTO> register(@RequestBody @Valid UserRegisterDTO registerDTO) {
        return ApiResponse.<UserResponseDTO>builder()
                .result(authService.register(registerDTO))
                .build();
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody @Valid LoginDTO loginDTO) {
        return ApiResponse.<AuthResponse>builder()
                .result(authService.login(loginDTO))
                .build();
    }
 @GetMapping("/login-success")
public ApiResponse<AuthResponse> loginSuccess(Authentication authentication) {
    // 1. Kiểm tra xác thực
    if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User oAuth2User)) {
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    // 2. Lấy email từ Google
    String email = oAuth2User.getAttribute("email");
if (email == null) {
    // Facebook đăng ký bằng SĐT sẽ không có email, dùng ID để tạo định danh khớp với DB
    String fbId = oAuth2User.getAttribute("id");
    email = fbId + "@facebook.com";
}
    System.out.println(">>> OAuth2 Login thành công cho: " + email);

    // 3. Gọi Service để lấy đủ Token và thông tin User từ DB
    AuthResponse response = authService.generateTokenForOAuth2(email);

    return ApiResponse.<AuthResponse>builder()
            .code(1000)
            .result(response)
            .build();
}
    // === BỔ SUNG 2 HÀM CÒN THIẾU Ở ĐÂY ===

    @PostMapping("/forgot-password")
    public ApiResponse<String> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        authService.sendForgotPasswordEmail(request.getEmail());
        return ApiResponse.<String>builder()
                .result("Link khôi phục mật khẩu đã được gửi tới email của bạn.")
                .build();
    }

    @PostMapping("/reset-password")
    public ApiResponse<String> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ApiResponse.<String>builder()
                .result("Mật khẩu đã được cập nhật thành công.")
                .build();
    }

    @PostMapping("/change-password")
    public ApiResponse<String> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        authService.changePassword(request);
        return ApiResponse.<String>builder()
                .result("Đổi mật khẩu thành công! Vui lòng đăng nhập lại.")
                .build();
    }

    @PutMapping("/change-email")
    public ApiResponse<String> changeEmail(@RequestBody ChangeEmailRequest request) {
        authService.changeEmail(request);
        return ApiResponse.<String>builder()
                .result("Đổi email thành công. Vui lòng đăng nhập lại bằng email mới!")
                .build();
    }
}