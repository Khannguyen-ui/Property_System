package com.homeverse.notification.config;

import com.homeverse.notification.listener.UserPresenceChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final UserPresenceChannelInterceptor userPresenceChannelInterceptor;


    public WebSocketConfig(UserPresenceChannelInterceptor userPresenceChannelInterceptor) {
        this.userPresenceChannelInterceptor = userPresenceChannelInterceptor;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(userPresenceChannelInterceptor);   // ← DÒNG QUAN TRỌNG NHẤT
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/user");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-notifier")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}