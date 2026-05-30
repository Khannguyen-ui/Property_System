package com.homeverse.chat.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // 1. Chỉ chặn xét hỏi khi Frontend gửi lệnh CONNECT (bắt đầu kết nối)
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            // 2. Lấy Header Authorization do Frontend gửi lên trong khung STOMP
            List<String> authorization = accessor.getNativeHeader("Authorization");

            if (authorization != null && !authorization.isEmpty()) {
                String authHeader = authorization.get(0);
                if (authHeader.startsWith("Bearer ")) {
                    String jwt = authHeader.substring(7);

                    // =======================================================
                    // 3. CHECK REDIS BLACKLIST CHỐNG LOGOUT ẢO
                    // =======================================================
                    if (Boolean.TRUE.equals(redisTemplate.hasKey("BLACKLIST:" + jwt))) {
                        log.warn("[WebSocket] Token bị Blacklist! Cắt cáp ngay.");
                        throw new IllegalArgumentException("Token đã bị thu hồi!");
                    }

                    // 4. KIỂM TRA CHỮ KÝ VÀ HẠN DÙNG
                    if (jwtUtils.isTokenValidBasic(jwt)) {
                        String userEmail = jwtUtils.extractUsername(jwt);
                        String role = jwtUtils.extractRole(jwt);
                        String userId = jwtUtils.extractUserId(jwt);

                        // =======================================================
                        // 5. CHECK CỜ REQUIRE_REFRESH
                        // =======================================================
                        String staleKey = "require_refresh:" + userEmail;
                        if (Boolean.TRUE.equals(redisTemplate.hasKey(staleKey))) {
                            log.warn(" [WebSocket] Token đã cũ! Yêu cầu Frontend gọi Refresh.");
                            throw new IllegalArgumentException("Token_Stale");
                        }

                        // =======================================================
                        // 6. CẤP QUYỀN CHO PHIÊN WEBSOCKET NÀY
                        // =======================================================
                        String principal = (userId != null) ? userId : userEmail;
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                        // Set User vào Context của phiên Socket này
                        accessor.setUser(auth);
                        log.info("[WebSocket] Cho phép User ID: {} kết nối STOMP thành công!", principal);
                        return message;
                    }
                }
            }
            // Nếu không có Token hoặc Token sai -> Quăng lỗi đá ra ngoài
            log.error("[WebSocket] Kẻ gian không có Token hợp lệ! Từ chối kết nối.");
            throw new IllegalArgumentException("Chưa xác thực (Missing or Invalid Token)!");
        }

        return message; // Với các lệnh khác (SEND, SUBSCRIBE) thì cứ cho đi qua bình thường
    }
}