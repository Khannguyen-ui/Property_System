package com.homeverse.notification.service;

public interface FcmPushService {
    void sendToUser(Long userId, String title, String body, String type);
}
