package com.homeverse.notification.service.impl;

import com.homeverse.notification.dto.DeviceTokenRequest;
import com.homeverse.notification.entity.UserDeviceToken;
import com.homeverse.notification.repository.UserDeviceTokenRepository;
import com.homeverse.notification.service.DeviceTokenService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeviceTokenServiceImpl implements DeviceTokenService {

    private final UserDeviceTokenRepository repository;

    public void save(DeviceTokenRequest request) {
        UserDeviceToken deviceToken = repository
                .findByUserIdAndToken(request.getUserId(), request.getToken())
                .orElse(UserDeviceToken.builder()
                        .userId(request.getUserId())
                        .token(request.getToken())
                        .createdAt(LocalDateTime.now())
                        .build());

        deviceToken.setPlatform(request.getPlatform());
        deviceToken.setActive(true);
        deviceToken.setUpdatedAt(LocalDateTime.now());

        repository.save(deviceToken);
    }
}