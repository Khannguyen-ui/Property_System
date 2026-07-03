package com.homeverse.notification.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceTokenRequest {
    private Long userId;
    private String token;
    private String platform;
}