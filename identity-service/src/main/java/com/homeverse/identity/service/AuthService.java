package com.homeverse.identity.service;

import com.homeverse.identity.dto.request.ChangeEmailRequest;
import com.homeverse.identity.dto.request.ChangePasswordRequest;
import com.homeverse.identity.dto.request.LoginDTO;
import com.homeverse.identity.dto.request.UserRegisterDTO;
import com.homeverse.identity.dto.response.AuthResponse;
import com.homeverse.identity.dto.response.UserResponseDTO;

public interface AuthService {
    UserResponseDTO register(UserRegisterDTO registerDTO);
    AuthResponse login(LoginDTO loginDTO);
    AuthResponse generateTokenForOAuth2(String email);
    void sendForgotPasswordEmail(String email);
    void resetPassword(String token, String newPassword);
    void changePassword(ChangePasswordRequest request);
    void changeEmail(ChangeEmailRequest request);

}