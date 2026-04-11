package com.homeverse.payment.controller;

import com.homeverse.payment.service.ServicePackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
public class ServicePackageController {

    private final ServicePackageService servicePackageService;

    // API để chủ trọ nhấn "Mua gói hội viên"
    // POST /api/packages/buy-membership?userId=1&packageId=2
    @PostMapping("/buy-membership")
    public ResponseEntity<?> buyMembership(
            @RequestParam Long userId,
            @RequestParam Long packageId) {
        try {
            servicePackageService.buyMembership(userId, packageId);
            return ResponseEntity.ok(Map.of("message", "Nâng cấp gói hội viên thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}