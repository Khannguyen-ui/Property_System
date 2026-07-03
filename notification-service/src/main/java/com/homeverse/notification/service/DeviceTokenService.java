package com.homeverse.notification.service;

import com.homeverse.notification.dto.DeviceTokenRequest;

public interface DeviceTokenService {
    void save(DeviceTokenRequest request);
}
