package com.homeverse.payment.controller;

import com.homeverse.payment.entity.ServicePackage;
import com.homeverse.payment.service.ServicePackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/packages")
@RequiredArgsConstructor
public class AdminServicePackageController {

    private final ServicePackageService servicePackageService;

    // Chỉ Admin xem toàn bộ gói, gồm cả inactive
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ServicePackage>> getAllPackagesForAdmin() {
        return ResponseEntity.ok(servicePackageService.getAllPackagesForAdmin());
    }


    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER',)")
    public ResponseEntity<List<ServicePackage>> getAllActivePackages() {
        return ResponseEntity.ok(servicePackageService.getAllActivePackages());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServicePackage> createPackage(@RequestBody ServicePackage pkg) {
        return ResponseEntity.ok(servicePackageService.createPackage(pkg));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServicePackage> updatePackage(
            @PathVariable Long id,
            @RequestBody ServicePackage pkg) {
        return ResponseEntity.ok(servicePackageService.updatePackage(id, pkg));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deletePackage(@PathVariable Long id) {
        servicePackageService.deletePackage(id);
        return ResponseEntity.ok(Map.of("message", "Đã ẩn gói cước thành công!"));
    }
}