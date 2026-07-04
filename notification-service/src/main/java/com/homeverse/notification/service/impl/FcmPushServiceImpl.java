package com.homeverse.notification.service.impl;

import com.google.firebase.messaging.*;
import com.homeverse.notification.entity.UserDeviceToken;
import com.homeverse.notification.repository.UserDeviceTokenRepository;
import com.homeverse.notification.service.FcmPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmPushServiceImpl implements FcmPushService {

    private final UserDeviceTokenRepository tokenRepository;

    @Override
    public void sendToUser(Long userId, String title, String body, String type) {
        List<UserDeviceToken> tokens = tokenRepository.findByUserIdAndActiveTrue(userId);

        for (UserDeviceToken deviceToken : tokens) {
            sendToToken(deviceToken.getToken(), title, body, type);
        }
    }

    private void sendToToken(String token, String title, String body, String type) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("type", safe(type));

            String channelId = resolveChannelId(type);

            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(safe(title))
                            .setBody(safe(body))
                            .build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setChannelId(channelId)
                                    .setSound("default")
                                    .build())
                            .build())
                    .putAllData(data)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM sent successfully: {}", response);

        } catch (FirebaseMessagingException e) {
            log.error("FCM send failed: {}", e.getMessage());
        }
    }

    private String resolveChannelId(String type) {
        if ("CHAT_NEW".equalsIgnoreCase(type)) {
            return "chat";
        }

        if (type != null && type.startsWith("APPOINTMENT_")) {
            return "appointment";
        }

        return "default";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}