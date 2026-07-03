package com.homeverse.notification.controller;

import com.homeverse.notification.dto.DeviceTokenRequest;
import com.homeverse.notification.service.DeviceTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/notifications/device-token")
@RestController
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping
    public ResponseEntity<?> saveToken(@RequestBody DeviceTokenRequest request) {
        deviceTokenService.save(request);
        return ResponseEntity.ok("FCM token saved");
    }
}