package com.homeverse.identity.service.impl;

import com.homeverse.common.exception.AppException;
import com.homeverse.common.exception.ErrorCode;
import org.springframework.security.authentication.DisabledException;
import com.homeverse.identity.dto.request.*;
import com.homeverse.identity.dto.response.*;
import com.homeverse.identity.entity.PasswordResetToken;
import com.homeverse.identity.entity.UserCredential;
import com.homeverse.identity.mapper.UserMapper;
import com.homeverse.identity.repository.PasswordResetTokenRepository;
import com.homeverse.identity.repository.UserCredentialRepository;
import com.homeverse.identity.service.AuthService;
import com.homeverse.identity.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final PasswordResetTokenRepository tokenRepository;
    private final UserCredentialRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final JavaMailSender mailSender;

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;


    @Override
    @Transactional
    public UserResponseDTO register(UserRegisterDTO registerDTO) {
        if (userRepository.existsByEmail(registerDTO.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        UserCredential user = userMapper.toEntity(registerDTO);
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setRole(UserCredential.Role.USER);


        UserCredential savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    @Override
    public AuthResponse login(LoginDTO loginDTO) {
        try {
            // 1. Spring Security tự động lo việc check Email và Password
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new AppException(ErrorCode.PASSWORD_INCORRECT);
        } catch (DisabledException e) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        }
        // 2. Lấy thông tin User ra
        UserCredential user = userRepository.findByEmail(loginDTO.getEmail())
                // SỬA Ở ĐÂY: Dùng AppException + ErrorCode
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (!user.isActive()) {
            // SỬA Ở ĐÂY: Dùng AppException + ErrorCode
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        }

        // 3. Đóng gói thêm userId
        java.util.Map<String, Object> extraClaims = new java.util.HashMap<>();
        extraClaims.put("userId", user.getId());

        // 4. Sinh Token có chứa userId
        String token = jwtUtils.generateToken(extraClaims, user);
        // =======================

        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    @Override
    @Transactional
    public void sendForgotPasswordEmail(String email) {
        UserCredential user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 1. Tạo token và lưu DB
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .build();
        tokenRepository.save(resetToken);

        // 🚨 1. LƯU LẠI CLASS LOADER CỦA LUỒNG CHÍNH (SPRING BOOT)
        ClassLoader springBootClassLoader = Thread.currentThread().getContextClassLoader();

        // 2. Chạy luồng ngầm
        CompletableFuture.runAsync(() -> {
            // Lấy class loader hiện tại của luồng ngầm (để tí trả lại)
            ClassLoader originalThreadClassLoader = Thread.currentThread().getContextClassLoader();

            try {
                // 🚨 2. TRUYỀN CÔNG LỰC: Ép luồng ngầm dùng Class Loader của Spring Boot
                Thread.currentThread().setContextClassLoader(springBootClassLoader);

                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setTo(email);
                helper.setSubject("[HomeVerse] Khôi phục mật khẩu tài khoản");

                String resetLink = frontendUrl + "/reset-password?token=" + token;

                String htmlMsg = "<div style='font-family: Arial, sans-serif; padding: 20px;'>"
                        + "<h2>Xin chào " + user.getFullName() + "!</h2>"
                        + "<p>Hệ thống HomeVerse vừa nhận được yêu cầu khôi phục mật khẩu từ tài khoản của bạn.</p>"
                        + "<p>Vui lòng click vào nút bên dưới để đặt lại mật khẩu. Link này chỉ có hiệu lực trong vòng <b>15 phút</b>.</p>"
                        + "<a href='" + resetLink + "' style='display: inline-block; padding: 10px 20px; background-color: #007bff; color: white; text-decoration: none; border-radius: 5px; font-weight: bold;'>ĐẶT LẠI MẬT KHẨU</a>"
                        + "<p style='margin-top: 20px; color: #555;'>Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này. Tài khoản của bạn vẫn an toàn.</p>"
                        + "</div>";

                helper.setText(htmlMsg, true);
                mailSender.send(message);

                log.info("✅ Đã gửi email khôi phục mật khẩu thành công tới: {}", email);
            } catch (Exception e) {
                log.error("❌ Lỗi khi gửi email cho {}: {}", email, e.getMessage());
            } finally {
                // 🚨 3. TRẢ LẠI NGUYÊN TRẠNG: Dọn dẹp Class Loader sau khi gửi xong
                Thread.currentThread().setContextClassLoader(originalThreadClassLoader);
            }
        });
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {

        PasswordResetToken resetToken = tokenRepository.findByToken(token)

                .orElseThrow(() -> new AppException(ErrorCode.TOKEN_INVALID));


        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }


        UserCredential user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);


        tokenRepository.delete(resetToken);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {

        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        UserCredential user = userRepository.findByEmail(currentEmail)

                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {

            throw new AppException(ErrorCode.PASSWORD_INCORRECT);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changeEmail(ChangeEmailRequest request) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        UserCredential user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.PASSWORD_INCORRECT);
        }

        if (userRepository.existsByEmail(request.getNewEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        user.setEmail(request.getNewEmail());
        userRepository.save(user);
    }
   @Override
public AuthResponse generateTokenForOAuth2(String email) {
    // Tìm trong DB
    var user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("KHONG TIM THAY USER TRONG DB: " + email));

    // Tạo token (Đảm bảo jwtUtils đã được @RequiredArgsConstructor tiêm vào)
    String jwtToken = jwtUtils.generateToken(user);

    return AuthResponse.builder()
            .token(jwtToken)
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(user.getRole().name())
            .build();
}
}
