package com.homeverse.identity.service.impl;

import com.homeverse.common.exception.AppException;
import com.homeverse.common.exception.ErrorCode;
import com.homeverse.identity.dto.response.AdminUserResponse;
import com.homeverse.identity.entity.KycAuditLog;
import com.homeverse.identity.entity.UserCredential;
import com.homeverse.identity.repository.KycAuditLogRepository;
import com.homeverse.identity.repository.UserCredentialRepository;
import com.homeverse.identity.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final KycAuditLogRepository auditLogRepository;
    private final UserCredentialRepository userRepository;

    private UserCredential findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private String getCurrentAdminName() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            return "UNKNOWN_ADMIN";
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserCredential admin) {
            return admin.getEmail();
        }

        return authentication.getName();
    }

    private void preventAdminTarget(UserCredential user) {
        if (user.getRole() == UserCredential.Role.ADMIN) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    private void saveAudit(Long userId, String action, String reason) {
        KycAuditLog log = KycAuditLog.builder()
                .userId(userId)
                .action(action)
                .performedBy(getCurrentAdminName())
                .reason(reason)
                .build();

        auditLogRepository.save(log);
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long userId) {
        UserCredential user = findUserById(userId);
        preventAdminTarget(user);

        user.setActive(!user.isActive());
        userRepository.save(user);

        saveAudit(
                userId,
                user.isActive() ? "UNLOCK_ACCOUNT" : "LOCK_ACCOUNT",
                user.isActive() ? "Admin mở khóa tài khoản" : "Admin khóa tài khoản"
        );
    }

    @Override
    @Transactional
    public void disableUser(Long userId) {
        UserCredential user = findUserById(userId);
        preventAdminTarget(user);

        user.setActive(false);
        userRepository.save(user);

        saveAudit(userId, "DISABLE_ACCOUNT", "Admin vô hiệu hóa tài khoản thay vì xóa cứng");
    }

    @Override
    @Transactional
    public void promoteToAdmin(Long userId) {
        UserCredential user = findUserById(userId);
        preventAdminTarget(user);

        user.setRole(UserCredential.Role.ADMIN);
        userRepository.save(user);

        saveAudit(userId, "PROMOTE_ADMIN", "Admin cấp quyền quản trị cho tài khoản");
    }

    @Override
    public List<AdminUserResponse> getPendingKycUsers() {
        return userRepository.findByKycStatus("PENDING")
                .stream()
                .filter(user -> user.getRole() != UserCredential.Role.ADMIN)
                .map(this::mapToAdminUserResponse)
                .toList();
    }

    @Override
    @Transactional
    public void approveKyc(Long userId) {
        UserCredential user = findUserById(userId);
        preventAdminTarget(user);

        user.setKycStatus("VERIFIED");

        if (user.getRole() == UserCredential.Role.USER) {
            user.setRole(UserCredential.Role.OWNER);
        }

        userRepository.save(user);

        saveAudit(userId, "MANUAL_APPROVE", "Admin duyệt hồ sơ KYC hợp lệ");
    }

    @Override
    @Transactional
    public void rejectKyc(Long userId, String reason) {
        UserCredential user = findUserById(userId);
        preventAdminTarget(user);

        user.setKycStatus("REJECTED");
        userRepository.save(user);

        saveAudit(userId, "MANUAL_REJECT", reason);
    }

    @Override
    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .filter(user -> user.getRole() != UserCredential.Role.ADMIN)
                .map(this::mapToAdminUserResponse)
                .toList();
    }

    private AdminUserResponse mapToAdminUserResponse(UserCredential user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .publicId(user.getPublicId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .kycStatus(user.getKycStatus())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .freePostsRemaining(user.getFreePostsRemaining())
                .build();
    }
}