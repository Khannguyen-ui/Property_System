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

    // 🟢 MỞ QUYỀN: Cho phép cả Admin và Landlord xem danh sách để chọn mua
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LANDLORD')")
    public ResponseEntity<List<ServicePackage>> getAllPackages() {
        return ResponseEntity.ok(servicePackageService.getAllPackagesForAdmin());
    }

    // 🔴 KHÓA CHẶT: Chỉ Admin mới được tạo gói mới
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServicePackage> createPackage(@RequestBody ServicePackage pkg) {
        return ResponseEntity.ok(servicePackageService.createPackage(pkg));
    }

    // 🔴 KHÓA CHẶT: Chỉ Admin mới được sửa giá/thông tin gói
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServicePackage> updatePackage(
            @PathVariable Long id, 
            @RequestBody ServicePackage pkg) {
        return ResponseEntity.ok(servicePackageService.updatePackage(id, pkg));
    }

    // 🔴 KHÓA CHẶT: Chỉ Admin mới được xóa gói
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deletePackage(@PathVariable Long id) {
        servicePackageService.deletePackage(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa gói cước thành công!"));
    }
    
}