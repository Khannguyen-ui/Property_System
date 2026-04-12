package com.homeverse.identity.service;

public interface UserService {
    void usePostQuota(Long userId);
    void refundPostQuota(Long userId);
}