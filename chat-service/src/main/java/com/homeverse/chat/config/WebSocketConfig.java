    package com.homeverse.chat.config;

    import com.homeverse.chat.security.WebSocketAuthInterceptor;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.messaging.simp.config.ChannelRegistration;
    import org.springframework.messaging.simp.config.MessageBrokerRegistry;
    import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
    import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
    import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

    @Configuration
    @EnableWebSocketMessageBroker // Quan trọng: Kích hoạt Message Broker để tạo ra SimpMessagingTemplate
    public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
        @Autowired
        private  WebSocketAuthInterceptor authInterceptor;

        @Override
        public void configureMessageBroker(MessageBrokerRegistry config) {
            // Các tin nhắn có prefix /topic sẽ được gửi tới Broker để bắn xuống Client
            config.enableSimpleBroker("/topic");
            
            // Các tin nhắn Client gửi lên bắt đầu bằng /app sẽ vào các hàm @MessageMapping
            config.setApplicationDestinationPrefixes("/app");
        }

        @Override
        public void registerStompEndpoints(StompEndpointRegistry registry) {
            // Đăng ký endpoint để Client kết nối WebSocket (Khớp với cấu hình Nginx /ws-chat)
            registry.addEndpoint("/ws-chat")
                    .setAllowedOrigins("*") // Lưu ý: Chỉnh lại domain cụ thể khi lên Prod
                    .withSockJS(); // Hỗ trợ fallback cho các trình duyệt cũ
            
            // Thêm một endpoint không có SockJS nếu FE dùng thư viện Stomp thuần
            registry.addEndpoint("/ws-chat").setAllowedOrigins("*");
        }
        @Override
        public void configureClientInboundChannel(ChannelRegistration registration) {
            registration.interceptors(authInterceptor);
        }
    }