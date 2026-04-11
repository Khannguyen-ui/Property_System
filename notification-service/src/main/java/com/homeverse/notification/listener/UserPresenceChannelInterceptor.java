package com.homeverse.notification.listener;

import com.homeverse.notification.security.JwtValidator;
import com.homeverse.notification.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserPresenceChannelInterceptor implements ChannelInterceptor {

    private final UserPresenceService presenceService;
    private final JwtValidator jwtValidator;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        StompCommand command = accessor.getCommand();

        String sessionId = accessor.getSessionId();


        if (StompCommand.CONNECT.equals(command)) {
            log.info("=== CHANNEL INTERCEPTOR - XÁC THỰC JWT ===");
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                if (jwtValidator.validateToken(token)) {
                    String userId = jwtValidator.getUserIdFromToken(token);

                    Principal principal = new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                    accessor.setUser(principal);


                    accessor.getSessionAttributes().put("userId", userId);


                    presenceService.setOnline(userId, sessionId);

                    log.info("XÁC THỰC THÀNH CÔNG - Gắn thẻ VIP cho userId = {}", userId);
                } else {
                    log.error("Token không hợp lệ. Từ chối kết nối!");
                    return null;
                }
            } else {
                log.error("Không tìm thấy header Authorization!");
                throw new IllegalArgumentException("Missing Authorization Header");
            }
        }


        else if (StompCommand.DISCONNECT.equals(command)) {
            String userId = (String) accessor.getSessionAttributes().get("userId");
            if (userId != null) {

                presenceService.setOffline(userId, sessionId);
                log.info(" SET OFFLINE THÀNH CÔNG - userId = {}", userId);
            }
        }

        return message;
    }
}