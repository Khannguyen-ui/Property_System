package com.homeverse.identity.service.impl;

import com.homeverse.common.exception.AppException;
import com.homeverse.common.exception.ErrorCode;
import com.homeverse.identity.entity.UserCredential;
import com.homeverse.identity.repository.UserCredentialRepository;
import com.homeverse.identity.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserCredentialRepository userRepository;

    @Override
    @Transactional
    public void usePostQuota(Long userId) {
        UserCredential user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getFreePostsRemaining() != null && user.getFreePostsRemaining() > 0) {
            user.setFreePostsRemaining(user.getFreePostsRemaining() - 1);
            userRepository.save(user);
            log.info("User ID {} đã sử dụng 1 lượt đăng. Còn lại: {}", userId, user.getFreePostsRemaining());
        } else {
            log.warn("User ID {} đã hết lượt đăng miễn phí!", userId);
            throw new AppException(ErrorCode.POST_LIMIT_EXCEEDED);
        }
    }

    @Override
    @Transactional
    public void refundPostQuota(Long userId) {
        UserCredential user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        int currentQuota = user.getFreePostsRemaining() == null ? 0 : user.getFreePostsRemaining();

        // Cộng lại 1 lượt đăng (chặn mức tối đa là 8 để user không bị buff lố)
        if (currentQuota < 8) {
            user.setFreePostsRemaining(currentQuota + 1);
            userRepository.save(user);
            log.info("Đã hoàn lại 1 lượt đăng cho User ID {}. Hiện có: {}", userId, user.getFreePostsRemaining());
        }
    }
}