package com.homeverse.property.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.homeverse.property.entity.PromotionQueue;

public interface PromotionQueueRepository extends JpaRepository<PromotionQueue, Long> {
    // Tìm gói đang chạy cho một bài đăng
    Optional<PromotionQueue> findFirstByPropertyIdAndStatusOrderByPriorityLevelDesc(Long propertyId,
            PromotionQueue.PromotionStatus status);

    // Thêm vào PromotionQueueRepository.java
    Optional<PromotionQueue> findFirstByPropertyIdAndStatusOrderByPriorityLevelDescCreatedAtAsc(
            Long propertyId,
            PromotionQueue.PromotionStatus status);

    // Tìm gói tiếp theo đang chờ
    List<PromotionQueue> findByPropertyIdAndStatusOrderByPriorityLevelDescCreatedAtAsc(Long propertyId,
            PromotionQueue.PromotionStatus status);

    Optional<PromotionQueue> findFirstByUserIdAndStatusOrderByPriorityLevelDesc(
            Long userId,
            PromotionQueue.PromotionStatus status);
}
