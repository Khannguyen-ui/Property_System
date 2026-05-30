package com.homeverse.identity.kafka.consumer;

import com.homeverse.identity.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyQuotaConsumer {

    private final UserService userService;

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
}