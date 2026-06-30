package com.homeverse.identity.security;

import com.homeverse.identity.repository.UserCredentialRepository;
import com.homeverse.identity.service.OAuth2LoginCodeService;
import com.homeverse.identity.util.JwtUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtils jwtUtils;
    private final UserCredentialRepository userRepository;
    private final OAuth2LoginCodeService oAuth2LoginCodeService;

    @Value("${frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String fbId = oAuth2User.getAttribute("id");

        if (email == null || email.isBlank()) {
            email = (fbId != null && !fbId.isBlank()) ? fbId + "@facebook.com" : null;
        }

        if (email == null) {
            throw new RuntimeException("Cannot resolve OAuth2 email");
        }

        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());
        extraClaims.put("role", user.getRole().name());

        String platform = request.getParameter("platform");
        String redirectUri = request.getParameter("redirect_uri");

        HttpSession session = request.getSession(false);
        String sessionTarget = null;
        if (session != null) {
            Object target = session.getAttribute("OAUTH2_TARGET");
            if (target instanceof String s && !s.isBlank()) {
                sessionTarget = s;
            }
            session.removeAttribute("OAUTH2_TARGET");
        }

        boolean isMobile = "mobile".equalsIgnoreCase(platform)
                || (redirectUri != null && redirectUri.startsWith("homeswipe://"))
                || (sessionTarget != null && sessionTarget.startsWith("homeswipe://"));

        if (isMobile) {
            String mobileTarget = (sessionTarget != null && sessionTarget.startsWith("homeswipe://"))
                    ? sessionTarget
                    : (redirectUri != null && redirectUri.startsWith("homeswipe://"))
                    ? redirectUri
                    : "homeswipe://login-success";

            String code = oAuth2LoginCodeService.issueCode(email);
            String targetUrl = mobileTarget + "?code=" + URLEncoder.encode(code, StandardCharsets.UTF_8);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            return;
        }

        String token = jwtUtils.generateToken(extraClaims, user);
        String targetUrl = frontendUrl + "/login-success?token=" + token;
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}