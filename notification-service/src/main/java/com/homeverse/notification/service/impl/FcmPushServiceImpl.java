package com.homeverse.notification.service.impl;

import com.google.firebase.messaging.*;
import com.homeverse.notification.entity.UserDeviceToken;
import com.homeverse.notification.repository.UserDeviceTokenRepository;
import com.homeverse.notification.service.DeviceTokenService;
import com.homeverse.notification.service.FcmPushService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FcmPushServiceImpl implements FcmPushService  {

    private final UserDeviceTokenRepository tokenRepository;

    public void sendToUser(Long userId, String title, String body, String type) {
        List<UserDeviceToken> tokens = tokenRepository.findByUserIdAndActiveTrue(userId);

        for (UserDeviceToken deviceToken : tokens) {
            sendToToken(deviceToken.getToken(), title, body, type);
        }
    }

    private void sendToToken(String token, String title, String body, String type) {
        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("type", type)
                    .build();

            FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException e) {
            System.err.println("FCM send failed: " + e.getMessage());
        }
    }
}