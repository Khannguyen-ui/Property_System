package com.homeverse.identity.controller;

import com.homeverse.common.dto.ApiResponse;
import com.homeverse.identity.entity.UserCredential;
import com.homeverse.identity.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor

@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @PutMapping("/{id}/status")
    public ApiResponse<String> toggleUserStatus(@PathVariable Long id) {
        adminService.toggleUserStatus(id);
        return ApiResponse.<String>builder()
                .result("Đã thay đổi trạng thái hoạt động của tài khoản thành công.")
                .build();
    }

    @PutMapping("/{id}/promote")
    public ApiResponse<String> promoteToAdmin(@PathVariable Long id) {
        adminService.promoteToAdmin(id);
        return ApiResponse.<String>builder()
                .result("Đã cấp quyền Quản trị viên (ADMIN) cho tài khoản này.")
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ApiResponse.<String>builder()
                .result("Đã xóa vĩnh viễn tài khoản khỏi hệ thống.")
                .build();
    }
    @GetMapping("/kyc/pending")
    public ApiResponse<List<UserCredential>> getPendingKycUsers() {
        return ApiResponse.<List<UserCredential>>builder()
                .result(adminService.getPendingKycUsers())
                .build();
    }

    @PutMapping("/{id}/kyc/approve")
    public ApiResponse<String> approveUserKyc(@PathVariable("id") Long id) {
        adminService.approveKyc(id);
        return ApiResponse.<String>builder()
                .result("Đã duyệt hồ sơ KYC thành công cho User ID: " + id)
                .build();
    }


    @PutMapping("/{id}/kyc/reject")
    public ApiResponse<String> rejectUserKyc(
            @PathVariable("id") Long id,
            @RequestParam("reason") String reason) {

        adminService.rejectKyc(id, reason);
        return ApiResponse.<String>builder()
                .result("Đã từ chối hồ sơ KYC. Lý do: " + reason)
                .build();
    }
}