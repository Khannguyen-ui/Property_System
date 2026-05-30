package com.homeverse.identity.security;

import com.homeverse.identity.util.JwtUtils;
import com.homeverse.identity.repository.UserCredentialRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtils jwtUtils;
    private final UserCredentialRepository userRepository;

   @Override
public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, 
                                    Authentication authentication) throws IOException, ServletException {
    
    OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
    String email = oAuth2User.getAttribute("email");

    // ÉP LOG RA ĐỂ KIỂM TRA
    System.out.println("🔥 SUCCESS HANDLER ĐANG CHẠY CHO: " + email);

    // 1. Lấy user từ DB để lấy ID
    var user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

    // 2. Tạo Claims có userId
    Map<String, Object> extraClaims = new HashMap<>();
    extraClaims.put("userId", user.getId());
    extraClaims.put("role", user.getRole().name());

    // 3. Tạo Token xịn
    String token = jwtUtils.generateToken(extraClaims, user);

    String targetUrl = "http://localhost:5173/login-success?token=" + token;
    
    getRedirectStrategy().sendRedirect(request, response, targetUrl);
}
}