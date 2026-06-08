package com.homeverse.identity.service;

import com.homeverse.identity.dto.response.AdminUserResponse;

import java.util.List;

public interface AdminService {
    void toggleUserStatus(Long userId);
    void disableUser(Long userId);
    void promoteToAdmin(Long userId);
    void approveKyc(Long userId);
    void rejectKyc(Long userId, String reason);
    List<AdminUserResponse> getPendingKycUsers();
    List<AdminUserResponse> getAllUsers();
}