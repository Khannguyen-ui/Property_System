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
    @KafkaListener(topics = "payment-success-topic", groupId = "property-promotion-debug-final-v3") // Đổi hẳn sang group mới
    public void consumePromotion(String message) {
        // LOG 1: Check xem có nhận được String từ Kafka không
        log.info("🔥 [KAFKA-RECEIVE] Tin nhắn thô cập bến: {}", message);
        
        try {
            // LOG 2: Check xem parse JSON sang Object có lỗi không
            PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);
            log.info("📦 [EVENT-PARSED] Object sau khi parse: RoomId={}, Type={}, Days={}", 
                    event.getRoomId(), event.getType(), event.getDurationDays());

            // LOG 3: Check điều kiện IF
            if (event.getRoomId() == null) {
                log.error("⚠️ [ERROR] RoomId bị NULL! Check lại module Common và Payment Producer.");
                return;
            }

            if (!"ROOM_PROMOTION".equals(event.getType())) {
                log.warn("ℹ️ [SKIP] Type không phải ROOM_PROMOTION (Type nhận được: {}). Bỏ qua.", event.getType());
                return;
            }

            // LOG 4: Bắt đầu tìm kiếm trong DB
            log.info("🔍 [DB-SEARCH] Đang tìm Property ID: {} trong Database...", event.getRoomId());
            
            propertyRepository.findById(event.getRoomId()).ifPresentOrElse(property -> {
                log.info("🏠 [DB-FOUND] Đã tìm thấy phòng: {}. Trạng thái cũ isPromoted: {}", 
                        property.getTitle(), property.getIsPromoted());

                // CẬP NHẬT TRẠNG THÁI
                property.setIsPromoted(true); 
                
                LocalDateTime now = LocalDateTime.now();
                if (property.getPromotionExpiresAt() != null && property.getPromotionExpiresAt().isAfter(now)) {
                    property.setPromotionExpiresAt(property.getPromotionExpiresAt().plusDays(event.getDurationDays()));
                    log.info("➕ [GIA-HAN] Cộng dồn ngày. Hạn mới: {}", property.getPromotionExpiresAt());
                } else {
                    property.setPromotionExpiresAt(now.plusDays(event.getDurationDays()));
                    log.info("✨ [MOI-TAO] Kích hoạt mới. Hạn đến: {}", property.getPromotionExpiresAt());
                }
                
                // LOG 5: Lưu xuống DB
                propertyRepository.save(property);
                log.info("✅ [SUCCESS] Đã lưu vào DB thành công cho: {}", property.getTitle());
                
            }, () -> log.error("❌ [NOT-FOUND] Không tìm thấy bài đăng ID: {} trong bảng properties!", event.getRoomId()));

        } catch (Exception e) {
            log.error("💥 [CRITICAL-ERROR] Lỗi nghiêm trọng: ", e); // In cả StackTrace ra luôn
        }
    }
}