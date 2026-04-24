package com.homeverse.property.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeverse.common.dto.PaymentEvent;
import com.homeverse.property.entity.Property;
import com.homeverse.property.entity.PromotionQueue;
import com.homeverse.property.repository.PromotionQueueRepository;
import com.homeverse.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionConsumer {

    private final PromotionQueueRepository promotionQueueRepository;
    private final PropertyRepository propertyRepository;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    @KafkaListener(topics = "payment-success-topic", groupId = "property-promotion-group-v3")
    public void consumePromotion(String message) {
        log.info("🔥 [KAFKA-RECEIVE] Tin nhắn: {}", message);

        try {
            PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);

            if (!"ROOM_PROMOTION".equals(event.getType())) {
                return;
            }

            Property property = propertyRepository.findById(event.getRoomId())
                    .orElseThrow(() -> new RuntimeException("Property not found ID: " + event.getRoomId()));

            PromotionQueue newQueueItem = PromotionQueue.builder()
                    .propertyId(event.getRoomId())
                    .userId(event.getUserId())
                    .packageId(event.getPackageId())
                    .packageName(event.getPackageName())
                    .priorityLevel(event.getPriorityLevel())
                    .durationDays(event.getDurationDays())
                    .amount(event.getAmount())
                    .transactionId(event.getTransactionId())
                    .status(PromotionQueue.PromotionStatus.WAITING)
                    .createdAt(LocalDateTime.now())
                    .build();

            boolean hasActive = promotionQueueRepository
                    .findFirstByPropertyIdAndStatusOrderByPriorityLevelDesc(event.getRoomId(),
                            PromotionQueue.PromotionStatus.ACTIVE)
                    .isPresent();

            if (!hasActive && property.getStatus() == Property.Status.ACTIVE) {
                LocalDateTime now = LocalDateTime.now();
                newQueueItem.setStatus(PromotionQueue.PromotionStatus.ACTIVE);
                newQueueItem.setActivatedAt(now);
                newQueueItem.setExpiresAt(now.plusDays(event.getDurationDays()));

                property.setIsPromoted(true);
                property.setPromotionPackageId(event.getPackageId());
                property.setPromotionPackageName(event.getPackageName());
                property.setPromotionExpiresAt(newQueueItem.getExpiresAt());
                
                log.info("✨ [ACTIVATE-NOW] Activated VIP for ACTIVE property {}", event.getRoomId());
            } else {
                log.info("⏳ [QUEUE] Property is PENDING or has ACTIVE package, queued for bài {}", event.getRoomId());
            }

            property.setQuotaDeducted(true);
            
            propertyRepository.saveAndFlush(property);
            promotionQueueRepository.save(newQueueItem);

            log.info("✅ [SUCCESS] ID: {}, is_quota_deducted: {}, is_promoted: {}", 
                    property.getId(), property.isQuotaDeducted(), property.getIsPromoted());

        } catch (Exception e) {
            log.error("💥 [ERROR] Promotion processing failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}