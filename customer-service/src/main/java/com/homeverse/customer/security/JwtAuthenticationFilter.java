package com.homeverse.customer.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        // =========================================================
        // 1. CỬA SỐ 1: CHECK SỔ ĐEN (BLACKLIST) CHỐNG LOGOUT ẢO
        // =========================================================
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey("BLACKLIST:" + jwt))) {
                log.warn(" [Customer Service] Kẻ gian dùng Token đã bị Logout/Thu hồi. Chặn ngay!");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\": 401, \"message\": \"UNAUTHENTICATED\", \"result\": \"Token đã bị thu hồi, vui lòng đăng nhập lại\"}");
                return; // Đá ra ngoài!
            }
        } catch (Exception e) {
            log.error("Lỗi Redis khi check Blacklist: {}", e.getMessage());
        }

        try {
            if (jwtUtils.isTokenValidBasic(jwt)) {
                String userEmail = jwtUtils.extractUsername(jwt);
                String role = jwtUtils.extractRole(jwt);
                String userId = jwtUtils.extractUserId(jwt);

                // =========================================================
                // 🟢 2. CỬA SỐ 2: CHECK CỜ "BỊ THIU" (SILENT REFRESH)
                // =========================================================
                try {
                    String staleKey = "require_refresh:" + userEmail;
                    if (Boolean.TRUE.equals(redisTemplate.hasKey(staleKey))) {
                        log.warn("⏳ [Customer Service] Token của {} đã để quá, ép Frontend đổi Token mới!", userEmail);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"code\": 4011, \"message\": \"TOKEN_STALE\", \"result\": \"Quyền hạn đã thay đổi, vui lòng làm mới token\"}");
                        return; // Ép đi lấy thẻ mới!
                    }
                } catch (Exception e) {
                    log.error("Lỗi Redis khi check cờ Refresh: {}", e.getMessage());
                }

                // =========================================================
                // 3. CẤP QUYỀN ĐI TIẾP VÀO BÊN TRONG
                // =========================================================
                if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // Dùng userId làm định danh chính (principal) để Controller dễ lấy ra dùng
                    String principal = (userId != null) ? userId : userEmail;

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("Xác thực thành công cho ID: {} (Email: {}, Role: {})", userId, userEmail, role);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi xác thực JWT: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}