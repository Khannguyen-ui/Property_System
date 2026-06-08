package com.homeverse.payment.repository;

import com.homeverse.payment.entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    Optional<UserSubscription> findFirstByUserIdAndActiveTrueAndExpiresAtAfterOrderByExpiresAtDesc(
            Long userId,
            LocalDateTime now
    );

    List<UserSubscription> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<UserSubscription> findByUserIdAndActiveTrue(Long userId);
}