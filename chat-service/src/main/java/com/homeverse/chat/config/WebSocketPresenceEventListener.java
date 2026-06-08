package com.homeverse.chat.config;

import com.homeverse.chat.service.ChatPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketPresenceEventListener {

    private final ChatPresenceService chatPresenceService;

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(event.getMessage());

        Object principal = accessor.getUser();

        Long userId = extractUserId(principal);

        if (userId != null) {
            chatPresenceService.markOnline(userId);
            log.info("User {} online", userId);
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(event.getMessage());

        Object principal = accessor.getUser();

        Long userId = extractUserId(principal);

        if (userId != null) {
            chatPresenceService.markOffline(userId);
            log.info("User {} offline", userId);
        }
    }

    private Long extractUserId(Object principal) {
        try {
            if (principal == null) {
                return null;
            }

            if (principal instanceof Jwt jwt) {
                return jwt.getClaim("userId");
            }

            return Long.valueOf(principal.toString());

        } catch (Exception e) {
            return null;
        }
    }
}