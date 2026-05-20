package com.homeverse.identity.service.impl;

import com.homeverse.common.exception.AppException;
import com.homeverse.common.exception.ErrorCode;
import com.homeverse.identity.entity.UserCredential;
import com.homeverse.identity.repository.UserCredentialRepository;
import com.homeverse.identity.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.homeverse.identity.entity.KycAuditLog;
import com.homeverse.identity.repository.KycAuditLogRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import com.homeverse.identity.dto.response.AdminUserResponse;


import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final KycAuditLogRepository auditLogRepository;
    private final UserCredentialRepository userRepository;

    private UserCredential findUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long userId) {
        UserCredential user = findUserById(userId);

        if (user.getRole() == UserCredential.Role.ADMIN) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }


        user.setActive(!user.isActive());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        UserCredential user = findUserById(userId);

        if (user.getRole() == UserCredential.Role.ADMIN) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        userRepository.delete(user);
    }

    @Override
    @Transactional
    public void promoteToAdmin(Long userId) {
        UserCredential user = findUserById(userId);
        user.setRole(UserCredential.Role.ADMIN);
        userRepository.save(user);
    }

    @Override
    public List<UserCredential> getPendingKycUsers() {
        return userRepository.findByKycStatus("PENDING");
    }

    @Override
    @Transactional
    public void approveKyc(Long userId) {
        UserCredential user = findUserById(userId);
        user.setKycStatus("VERIFIED");

        if (user.getRole() == UserCredential.Role.USER) {
            user.setRole(UserCredential.Role.OWNER);
        }
        userRepository.save(user);


        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();


        KycAuditLog log = KycAuditLog.builder().userId(userId).action("MANUAL_APPROVE").performedBy(adminEmail).reason("Admin duyệt hồ sơ hợp lệ").build();
        auditLogRepository.save(log);


    }

    @Override
    @Transactional
    public void rejectKyc(Long userId, String reason) {
        UserCredential user = findUserById(userId);
        user.setKycStatus("REJECTED");
        userRepository.save(user);

        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();


        KycAuditLog log = KycAuditLog.builder().userId(userId).action("MANUAL_REJECT").performedBy(adminEmail).reason(reason).build();
        auditLogRepository.save(log);

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
                .email(user.getEmail())
                .role(user.getRole())
                .kycStatus(user.getKycStatus())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .freePostsRemaining(user.getFreePostsRemaining())
                .build();
    }
}