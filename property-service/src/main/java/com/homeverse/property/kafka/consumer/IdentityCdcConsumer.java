package com.homeverse.property.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeverse.property.entity.OwnerQuota;
import com.homeverse.property.repository.OwnerQuotaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityCdcConsumer {

    private final ObjectMapper objectMapper;
    private final OwnerQuotaRepository quotaRepository;

    @KafkaListener(topics = "identity_server.public.user_credentials", groupId = "property-group")
    @Transactional
    public void consumeIdentityChanges(String message) {
        try {
            JsonNode rootNode = objectMapper.readTree(message);
            JsonNode payloadNode = rootNode.has("payload") ? rootNode.get("payload") : rootNode;

            if (payloadNode == null || payloadNode.isNull()) return;

            String operation = payloadNode.path("op").asText("");
            JsonNode afterNode = payloadNode.path("after");

            if (afterNode.isMissingNode() || afterNode.isNull()) return;

            Long ownerId = afterNode.path("id").asLong();
            String role = afterNode.path("role").asText("USER");

            if (!"c".equals(operation) && !"r".equals(operation) && !"u".equals(operation)) {
                return;
            }

            OwnerQuota quota = quotaRepository.findById(ownerId)
                    .orElseGet(() -> OwnerQuota.builder()
                            .ownerId(ownerId)
                            .freePostsRemaining(0)
                            .role(role)
                            .build());

            if (quota.getRole() == null || quota.getRole().isBlank() || isRoleUpgraded(role, quota.getRole())) {
                quota.setRole(role);
            }

            quotaRepository.save(quota);

            log.info(
                    "CDC Identity synced user {} role={}, quota giữ nguyên={}",
                    ownerId,
                    quota.getRole(),
                    quota.getFreePostsRemaining()
            );
        } catch (Exception e) {
            log.error("Lỗi đồng bộ CDC từ Identity: {}", e.getMessage(), e);
        }
    }

    private boolean isRoleUpgraded(String incomingRole, String currentRole) {
        if (incomingRole == null || incomingRole.isBlank()) return false;
        if (currentRole == null || currentRole.isBlank()) return true;

        if ("OWNER".equalsIgnoreCase(incomingRole) && "USER".equalsIgnoreCase(currentRole)) {
            return true;
        }

        if ("ADMIN".equalsIgnoreCase(incomingRole)) {
            return true;
        }

        return false;
    }
}