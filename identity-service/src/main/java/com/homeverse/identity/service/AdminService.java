package com.homeverse.identity.service;

import com.homeverse.identity.entity.UserCredential;
import com.homeverse.identity.dto.response.AdminUserResponse;

import java.util.List;

public interface AdminService {
    void toggleUserStatus(Long userId);
    void deleteUser(Long userId);
    void promoteToAdmin(Long userId);
    void approveKyc(Long userId);
    void rejectKyc(Long userId, String reason);
    List<UserCredential> getPendingKycUsers();
    List<AdminUserResponse> getAllUsers();
}