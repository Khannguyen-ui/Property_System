package com.homeverse.property.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate; // 🟢 THÊM THƯ VIỆN REDIS
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
    private final StringRedisTemplate redisTemplate; // 🟢 BƠM REDIS VÀO ĐÂY

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");


        String path = request.getRequestURI();

if (path != null && path.startsWith("/actuator")) {
    filterChain.doFilter(request, response);
    return;
}

        final String jwt = authHeader.substring(7);


        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey("BLACKLIST:" + jwt))) {
                log.warn(" [Property Service] Phát hiện Token nằm trong Blacklist. Chặn Request!");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\": 401, \"message\": \"UNAUTHENTICATED\", \"result\": \"Token đã bị thu hồi, vui lòng đăng nhập lại\"}");
                return; // Khóa cửa ngay và luôn
            }
        } catch (Exception e) {
            log.error("Lỗi không thể kết nối Redis để check Blacklist: {}", e.getMessage());
        }

        try {
            if (jwtUtils.isTokenValidBasic(jwt)) {
                String userId = jwtUtils.extractUserId(jwt);
                String role = jwtUtils.extractRole(jwt);
                String userEmail = jwtUtils.extractUsername(jwt); // Lấy email để check cờ


                try {
                    String staleKey = "require_refresh:" + userEmail;
                    if (Boolean.TRUE.equals(redisTemplate.hasKey(staleKey))) {
                        log.warn(" [Property Service] Token của {} đã để quá, ép Frontend đổi Token mới!", userEmail);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"code\": 4011, \"message\": \"TOKEN_STALE\", \"result\": \"Quyền hạn đã thay đổi, vui lòng làm mới token\"}");
                        return; // Chặn chặn chặn! Đuổi về xin thẻ mới!
                    }
                } catch (Exception e) {
                    log.error("Lỗi không thể kết nối Redis để check Cờ Refresh: {}", e.getMessage());
                }

                // 🟢 BƯỚC 3: XÁC THỰC THÀNH CÔNG, CHO ĐI TIẾP
                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info(" Xác thực thành công cho User ID: {} với Role: {}", userId, role);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi xác thực JWT: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}