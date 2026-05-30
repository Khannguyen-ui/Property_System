package com.homeverse.wallet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate; // 🟢 THÊM REDIS
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

    private final JwtValidator jwtValidator;
    private final StringRedisTemplate redisTemplate; // 🟢 TIÊM REDIS VÀO ĐÂY

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // =========================================================
        // 🟢 GIỮ NGUYÊN LOGIC CỦA SẾP: Bỏ qua API nội bộ
        // =========================================================
        String path = request.getRequestURI();
        if (path != null && path.contains("/api/wallets/debit")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // =========================================================
        // 🟢 1. CỬA SỐ 1: CHECK SỔ ĐEN (BLACKLIST)
        // =========================================================
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey("BLACKLIST:" + token))) {
                log.warn(" [Wallet Service] Kẻ gian dùng Token đã bị Logout/Thu hồi. Chặn ngay!");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\": 401, \"message\": \"UNAUTHENTICATED\", \"result\": \"Token đã bị thu hồi, vui lòng đăng nhập lại\"}");
                return; // Khóa cửa!
            }
        } catch (Exception e) {
            log.error("Lỗi Redis khi check Blacklist: {}", e.getMessage());
        }

        try {
            if (jwtValidator.validateToken(token)) {
                String userId = jwtValidator.getUserIdFromToken(token);
                String userEmail = jwtValidator.getUserEmailFromToken(token);
                String role = jwtValidator.getRoleFromToken(token); // Lấy Role

                // =========================================================
                // 🟢 2. CỬA SỐ 2: CHECK CỜ "BỊ THIU" (SILENT REFRESH)
                // =========================================================
                try {
                    String staleKey = "require_refresh:" + userEmail;
                    if (Boolean.TRUE.equals(redisTemplate.hasKey(staleKey))) {
                        log.warn("[Wallet Service] Token của {} đã để quá hạn, ép Frontend đổi Token mới!", userEmail);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"code\": 4011, \"message\": \"TOKEN_STALE\", \"result\": \"Quyền hạn đã thay đổi, vui lòng làm mới token\"}");
                        return; // Ép đi lấy thẻ mới!
                    }
                } catch (Exception e) {
                    log.error("Lỗi Redis khi check cờ Refresh: {}", e.getMessage());
                }

                // =========================================================
                // 🟢 3. CẤP QUYỀN ĐI TIẾP VÀO BÊN TRONG
                // =========================================================
                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role)) // Cấp Role chuẩn chỉ
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info(" [Wallet Service] Xác thực thành công cho ID: {} (Email: {}, Role: {})", userId, userEmail, role);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi xác thực JWT Wallet: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}