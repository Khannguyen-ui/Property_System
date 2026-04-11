package com.homeverse.notification.kafka;


import com.homeverse.common.dto.PaymentEvent;
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
public class PaymentNotificationConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserPresenceService presenceService;
    private final NotificationRepository notificationRepository;

    @KafkaListener(topics = "payment-success-topic", groupId = "notification-group")
    public void consumePaymentSuccess(PaymentEvent event) {
        log.info("=== NHẬN TIN THANH TOÁN THÀNH CÔNG - UserId: {} ===", event.getUserId());
        
        try {
            String title = "Giao dịch thành công!";
            String content = "Bạn đã nạp thành công " + event.getAmount() + " VNĐ. Mã GD: " + event.getTransactionId();
            
            sendNotificationToUser(event.getUserId().toString(), "PAYMENT_SUCCESS", title, content);
            
        } catch (Exception e) {
            log.error("Lỗi xử lý thông báo thanh toán: {}", e.getMessage());
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
            log.info("Đã đẩy thông báo Real-time qua WebSocket cho user {}", userId);
        } else {
            log.info("User {} đang offline - Chỉ lưu thông báo vào Database", userId);
        }
    }
}