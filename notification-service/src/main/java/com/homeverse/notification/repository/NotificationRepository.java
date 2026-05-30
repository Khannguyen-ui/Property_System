package com.homeverse.notification.repository;

import com.homeverse.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {


    Page<Notification> findByUserId(String userId, Pageable pageable);


    Page<Notification> findByUserIdAndIsReadFalse(String userId, Pageable pageable);


    long countByUserIdAndIsReadFalse(String userId);


    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsReadByUserId(String userId);
}