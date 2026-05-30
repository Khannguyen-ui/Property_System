package com.homeverse.property.kafka.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeverse.common.kafka.cdc.DebeziumMessage;
import com.homeverse.common.kafka.cdc.message.CustomerCdcMessage;
import com.homeverse.property.entity.OwnerProfile;
import com.homeverse.property.repository.OwnerProfileRepository;
import com.homeverse.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerSyncDataConsumer {

    private final ObjectMapper objectMapper;
    private final PropertyRepository propertyRepository;
    private final OwnerProfileRepository ownerProfileRepository;

    @KafkaListener(topics = "customer_server.public.user_credentials", groupId = "property-group")
    @Transactional
    public void consume(String message) {
        try {
            JsonNode rootNode = objectMapper.readTree(message);
            JsonNode payloadNode = rootNode.has("payload") ? rootNode.get("payload") : rootNode;

            DebeziumMessage<CustomerCdcMessage> debeziumMessage = objectMapper.convertValue(
                    payloadNode,
                    new TypeReference<DebeziumMessage<CustomerCdcMessage>>() {}
            );

            if (debeziumMessage == null || debeziumMessage.getOp() == null) return;

            String operation = debeziumMessage.getOp();
            CustomerCdcMessage before = debeziumMessage.getBefore();
            CustomerCdcMessage after = debeziumMessage.getAfter();

            // =========================================================
            // 1. TẠO PROFILE KHI CÓ USER MỚI
            // =========================================================
            if (("c".equals(operation) || "r".equals(operation)) && after != null) {
                log.info("[CDC] Có User mới. Đang lưu OwnerProfile cho ID: {}", after.getId());

                // Sử dụng Builder pattern cực gọn từ Entity của bạn
                OwnerProfile profile = OwnerProfile.builder()
                        .id(after.getId())
                        .fullName(after.getFullName())
                        .avatar(after.getAvatarUrl())
                        .slug(after.getPublicId())
                        .phone(after.getPhone())
                        .build();

                ownerProfileRepository.save(profile);
                log.info("=> Đã lưu OwnerProfile thành công cho ID: {}", after.getId());
            }

            // =========================================================
            // 2. CẬP NHẬT PROFILE & SNAPSHOT KHI CÓ THAY ĐỔI
            // =========================================================
            if ("u".equals(operation) && after != null) {
                log.info("[CDC] Customer update event: id={}, phone={}", after.getId(), after.getPhone());

                OwnerProfile profile = ownerProfileRepository.findById(after.getId())
                        .orElseGet(() -> OwnerProfile.builder()
                                .id(after.getId())
                                .build());

                boolean isProfileChanged =
                        !isEqual(profile.getFullName(), after.getFullName()) ||
                                !isEqual(profile.getAvatar(), after.getAvatarUrl()) ||
                                !isEqual(profile.getSlug(), after.getPublicId()) ||
                                !isEqual(profile.getPhone(), after.getPhone());

                if (isProfileChanged) {
                    log.info("[CDC] Customer ID {} đổi Profile. Đang đồng bộ...", after.getId());

                    profile.setId(after.getId());
                    profile.setFullName(after.getFullName());
                    profile.setAvatar(after.getAvatarUrl());
                    profile.setSlug(after.getPublicId());
                    profile.setPhone(after.getPhone());

                    ownerProfileRepository.save(profile);

                    propertyRepository.updateOwnerSnapshot(
                            after.getId(),
                            after.getFullName(),
                            after.getAvatarUrl(),
                            after.getPublicId(),
                            after.getPhone()
                    );

                    log.info("=> Đồng bộ Profile và Snapshot thành công cho ID: {}", after.getId());
                }
            }

        } catch (Exception e) {
            log.error("Lỗi khi xử lý thông điệp CDC từ Customer: {}", e.getMessage(), e);
        }
    }

    private boolean isEqual(String str1, String str2) {
        if (str1 == null && str2 == null) return true;
        if (str1 == null || str2 == null) return false;
        return str1.equals(str2);
    }
}