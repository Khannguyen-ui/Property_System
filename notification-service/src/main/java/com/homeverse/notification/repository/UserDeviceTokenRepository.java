package com.homeverse.notification.repository;

import com.homeverse.notification.entity.UserDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserDeviceTokenRepository extends JpaRepository<UserDeviceToken, Long> {

    List<UserDeviceToken> findByUserIdAndActiveTrue(Long userId);

    Optional<UserDeviceToken> findByUserIdAndToken(Long userId, String token);
}