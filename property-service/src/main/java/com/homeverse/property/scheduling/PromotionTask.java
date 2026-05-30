package com.homeverse.property.scheduling;

import com.homeverse.property.entity.Property;
import com.homeverse.property.entity.PromotionQueue;
import com.homeverse.property.repository.PromotionQueueRepository;
import com.homeverse.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PromotionTask {

    private final PromotionQueueRepository promotionQueueRepository;
    private final PropertyRepository propertyRepository;

    @Scheduled(fixedRate = 60000) // Chạy mỗi 1 phút một lần
    @Transactional
    public void processExpiredPromotions() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Tìm tất cả các gói ACTIVE đã hết hạn
        List<PromotionQueue> expiredQueues = promotionQueueRepository.findAll().stream()
                .filter(q -> q.getStatus() == PromotionQueue.PromotionStatus.ACTIVE && q.getExpiresAt().isBefore(now))
                .toList();

        for (PromotionQueue expiredQueue : expiredQueues) {
            log.info("⏳ Gói VIP {} của bài đăng {} đã hết hạn.", expiredQueue.getPackageName(), expiredQueue.getPropertyId());

            // Đổi trạng thái gói cũ sang EXPIRED (Bạn có thể thêm status EXPIRED vào Enum nếu chưa có)
            expiredQueue.setStatus(PromotionQueue.PromotionStatus.EXPIRED); 
            promotionQueueRepository.save(expiredQueue);

            // 2. Tìm gói tiếp theo đang chờ (WAITING) của bài đăng này
            promotionQueueRepository.findFirstByPropertyIdAndStatusOrderByPriorityLevelDescCreatedAtAsc(
                    expiredQueue.getPropertyId(), 
                    PromotionQueue.PromotionStatus.WAITING
            ).ifPresentOrElse(nextQueue -> {
                // KÍCH HOẠT GÓI TIẾP THEO
                nextQueue.setStatus(PromotionQueue.PromotionStatus.ACTIVE);
                nextQueue.setActivatedAt(now);
                nextQueue.setExpiresAt(now.plusDays(nextQueue.getDurationDays()));
                promotionQueueRepository.save(nextQueue);

                // Cập nhật lại thông tin vào bảng Property
                propertyRepository.findById(nextQueue.getPropertyId()).ifPresent(p -> {
                    p.setIsPromoted(true);
                    p.setPromotionPackageId(nextQueue.getPackageId());
                    p.setPromotionPackageName(nextQueue.getPackageName());
                    p.setPromotionExpiresAt(nextQueue.getExpiresAt());
                    propertyRepository.save(p);
                });
                log.info("✨ Đã kích hoạt gói nối tiếp: {} cho bài đăng {}", nextQueue.getPackageName(), nextQueue.getPropertyId());

            }, () -> {
                // NẾU KHÔNG CÒN GÓI NÀO CHỜ -> Gỡ VIP của bài đăng
                propertyRepository.findById(expiredQueue.getPropertyId()).ifPresent(p -> {
                    p.setIsPromoted(false);
                    p.setPromotionPackageId(null);
                    p.setPromotionPackageName(null);
                    p.setPromotionExpiresAt(null);
                    propertyRepository.save(p);
                });
                log.info("❌ Bài đăng {} đã hết hạn VIP và không còn gói chờ.", expiredQueue.getPropertyId());
            });
        }
    }
}