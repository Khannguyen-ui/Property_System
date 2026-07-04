package com.homeverse.search.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (path != null && path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }
        final String authHeader = request.getHeader("Authorization");

        // Nếu không có Token thì cho qua, các Filter sau của Spring Security sẽ chặn
        // lại nếu cần

        final String jwt = authHeader.substring(7);

        try {
            if (jwtUtils.isTokenValidBasic(jwt)) {
                String userId = jwtUtils.extractUserId(jwt);
                String role = jwtUtils.extractRole(jwt);

                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // Nạp UserId và Role vào SecurityContext chuẩn Spring Security
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("Xác thực thành công cho User ID: {} với Role: {}", userId, role);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi xác thực JWT: {}", e.getMessage());

        }

        filterChain.doFilter(request, response);
    }
}