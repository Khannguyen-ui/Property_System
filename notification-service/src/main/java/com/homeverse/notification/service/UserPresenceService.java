package com.homeverse.notification.service;

public interface UserPresenceService {

    void setOnline(String userId, String sessionId);

    void setOffline(String userId, String sessionId);

    boolean isOnline(String userId);
}