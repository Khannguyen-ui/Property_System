package com.homeverse.notification.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeverse.notification.entity.Notification;
import com.homeverse.notification.repository.NotificationRepository;
import com.homeverse.notification.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserPresenceService presenceService;
    private final NotificationRepository notificationRepository;

    @KafkaListener(topics = "identity_server.public.user_credentials", groupId = "notification-group")
    public void consumeIdentityChanges(String message) {
        try {
            JsonNode rootNode = objectMapper.readTree(message);
            JsonNode payload = rootNode.path("payload").isMissingNode() ? rootNode : rootNode.path("payload");


            if (!"u".equals(payload.path("op").asText(""))) return;

            JsonNode after = payload.path("after");
            if (after.isMissingNode() || after.isNull()) return;

            String userId = after.path("id").asText();
            String newRole = after.path("role").asText("");


            String oldRole = payload.path("before").path("role").asText("");

            if (!"OWNER".equals(oldRole) && "OWNER".equals(newRole)) {
                log.info("User {} được thăng cấp OWNER!", userId);

                sendNotificationToUser(userId,
                        "KYC_APPROVED",
                        "Thăng cấp thành công!",
                        "Tài khoản của bạn đã được duyệt. Giờ đây bạn có thể bắt đầu đăng bài!");
            }
        } catch (Exception e) {
            log.error("Lỗi xử lý Kafka CDC Identity: {}", e.getMessage(), e);
        }
    }


    private void sendNotificationToUser(String userId, String type, String title, String content) {
        Notification notif = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .content(content)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification savedNotif = notificationRepository.save(notif);

        if (presenceService.isOnline(userId)) {
            messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", savedNotif);
            log.info("Gửi realtime WebSocket cho user {}", userId);
        } else {
            log.info(" User {} offline → chỉ lưu DB", userId);
        }
    }

}