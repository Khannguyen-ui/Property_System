package com.homeverse.identity.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeverse.common.dto.PropertyQuotaSyncEvent;
import com.homeverse.identity.entity.UserCredential;
import com.homeverse.identity.repository.UserCredentialRepository;
import com.homeverse.identity.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyQuotaConsumer {

    private final UserService userService;
    private final UserCredentialRepository userRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "deduct-quota-topic", groupId = "identity-group")
    public void consumeDeductQuotaEvent(String message) {
        try {
            Long ownerId = Long.valueOf(message);
            userService.usePostQuota(ownerId);
            log.info("Đã nhận Event và trừ lượt thành công cho Owner ID: {}", ownerId);
        } catch (Exception e) {
            log.error("Lỗi khi xử lý trừ Quota qua Kafka: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "property-quota-sync-topic", groupId = "identity-quota-sync-group")
    @Transactional
    public void consumeQuotaSyncEvent(String message) {
        try {
            PropertyQuotaSyncEvent event = objectMapper.readValue(message, PropertyQuotaSyncEvent.class);

            if (event.getUserId() == null) {
                log.warn("Bỏ qua quota sync vì thiếu userId: {}", message);
                return;
            }

            UserCredential user = userRepository.findById(event.getUserId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user ID: " + event.getUserId()));

            int quota = event.getFreePostsRemaining() != null ? event.getFreePostsRemaining() : 0;
            user.setFreePostsRemaining(quota);

            if (event.getRole() != null && !event.getRole().isBlank()) {
                try {
                    user.setRole(UserCredential.Role.valueOf(event.getRole()));
                } catch (IllegalArgumentException ignored) {
                    log.warn("Role không hợp lệ từ property-service: {}", event.getRole());
                }
            }

            userRepository.save(user);

            log.info(
                    "Đã sync quota từ property-service sang identity: userId={}, quota={}, role={}, reason={}",
                    event.getUserId(),
                    quota,
                    user.getRole(),
                    event.getReason()
            );
        } catch (Exception e) {
            log.error("Lỗi khi sync quota từ property-service sang identity: {}", e.getMessage(), e);
        }
    }
}