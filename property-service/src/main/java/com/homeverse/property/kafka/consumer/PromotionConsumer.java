package com.homeverse.property.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeverse.common.dto.PaymentEvent;
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

    private final PropertyRepository propertyRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    @KafkaListener(topics = "payment-success-topic", groupId = "property-promotion-group-v3")
    public void consumePromotion(String message) {
        log.info("🔥 [KAFKA-RECEIVE] Tin nhắn: {}", message);
        
        try {
            PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);

            if ("MEMBERSHIP".equals(event.getType())) {
                log.info("ℹ️ [SKIP] Loại MEMBERSHIP được xử lý bởi QuotaConsumer.");
                return;
            }

            if (!"ROOM_PROMOTION".equals(event.getType())) {
                log.warn("ℹ️ [SKIP] Type {} không thuộc phạm vi xử lý.", event.getType());
                return;
            }

            if (event.getRoomId() == null) {
                log.error("⚠️ [ERROR] RoomId null trong tin nhắn ROOM_PROMOTION!");
                return;
            }

            log.info("🔍 [DB-SEARCH] Tìm Property ID: {}", event.getRoomId());
            
            propertyRepository.findById(event.getRoomId()).ifPresentOrElse(property -> {
                log.info("🏠 [DB-FOUND] Đang xử lý: {}. Gói: {}", property.getTitle(), event.getPackageName());

                property.setIsPromoted(true);
                property.setPromotionPackageName(event.getPackageName()); 
                property.setPromotionPackageId(event.getPackageId());
                
                LocalDateTime now = LocalDateTime.now();
                if (property.getPromotionExpiresAt() != null && property.getPromotionExpiresAt().isAfter(now)) {
                    property.setPromotionExpiresAt(property.getPromotionExpiresAt().plusDays(event.getDurationDays()));
                    log.info("➕ [EXTEND] Hạn mới: {}", property.getPromotionExpiresAt());
                } else {
                    property.setPromotionExpiresAt(now.plusDays(event.getDurationDays()));
                    log.info("✨ [ACTIVATE] Hạn đến: {}", property.getPromotionExpiresAt());
                }
                
                propertyRepository.save(property);
                log.info("✅ [SUCCESS] Đã cập nhật gói dịch vụ cho: {}", property.getTitle());
                
            }, () -> log.error("❌ [NOT-FOUND] Không tìm thấy bài đăng ID: {}", event.getRoomId()));

        } catch (Exception e) {
            log.error("💥 [ERROR] Lỗi xử lý Promotion: {}", e.getMessage());
        }
    }
}