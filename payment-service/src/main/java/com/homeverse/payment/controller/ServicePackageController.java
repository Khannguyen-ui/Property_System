package com.homeverse.payment.controller;

import com.homeverse.payment.service.ServicePackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
public class ServicePackageController {

    private final ServicePackageService servicePackageService;

    @PostMapping("/buy-membership")
    public ResponseEntity<?> buyMembership(
            @RequestParam Long packageId, 
            Authentication authentication) {
        try {
         
            Long userId = Long.valueOf(authentication.getName());
            
            servicePackageService.buyMembership(userId, packageId);
            
            return ResponseEntity.ok(Map.of("message", "Nâng cấp gói hội viên thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/buy-promotion")
    public ResponseEntity<?> buyPromotion(
            @RequestParam Long packageId,
            @RequestParam Long propertyId,
            Authentication authentication) { 
        try {
            Long userId = Long.valueOf(authentication.getName());
            
            servicePackageService.buyPromotion(userId, packageId, propertyId);
            
            return ResponseEntity.ok(Map.of("message", "Mua gói đẩy tin thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}