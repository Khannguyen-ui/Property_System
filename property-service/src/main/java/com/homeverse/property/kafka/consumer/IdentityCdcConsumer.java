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
            Integer freePosts = afterNode.path("free_posts_remaining").asInt(0);
            String role = afterNode.path("role").asText("");

            if ("c".equals(operation) || "r".equals(operation) || "u".equals(operation)) {


                OwnerQuota quota = quotaRepository.findById(ownerId)
                        .orElse(new OwnerQuota(ownerId, freePosts, role));

                quota.setFreePostsRemaining(freePosts);
                quota.setRole(role);
                quotaRepository.save(quota);

                log.info("CDC Synced: Owner ID {} hiện có {} lượt đăng", ownerId, freePosts);
            }
        } catch (Exception e) {
            log.error("Lỗi đồng bộ CDC từ Identity: {}", e.getMessage(), e);
        }
    }
}