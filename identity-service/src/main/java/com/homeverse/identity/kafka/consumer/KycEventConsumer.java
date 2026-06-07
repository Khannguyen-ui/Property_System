package com.homeverse.identity.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeverse.common.dto.KycApprovedEvent;
import com.homeverse.identity.entity.UserCredential;
import com.homeverse.identity.repository.UserCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class KycEventConsumer {

    private final UserCredentialRepository userRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "kyc-approved-topic", groupId = "identity-group")
    @Transactional
    public void handleKycApprovedEvent(String payload) {
        log.info("Nhận KYC event từ customer-service: {}", payload);

        try {
            KycApprovedEvent event;

            if (payload != null && payload.trim().startsWith("{")) {
                event = objectMapper.readValue(payload, KycApprovedEvent.class);
            } else {
                event = KycApprovedEvent.builder()
                        .email(payload)
                        .kycStatus("VERIFIED")
                        .build();
            }

            UserCredential user = null;

            if (event.getUserId() != null) {
                user = userRepository.findById(event.getUserId()).orElse(null);
            }

            if (user == null && event.getEmail() != null && !event.getEmail().isBlank()) {
                user = userRepository.findByEmail(event.getEmail())
                        .orElseThrow(() -> new RuntimeException(
                                "Không tìm thấy user với email: " + event.getEmail()
                        ));
            }

            if (user == null) {
                throw new RuntimeException("Không tìm thấy user từ KYC event");
            }

            user.setRole(UserCredential.Role.OWNER);

            user.setKycStatus(
                    event.getKycStatus() != null && !event.getKycStatus().isBlank()
                            ? event.getKycStatus()
                            : "VERIFIED"
            );

            if (event.getFullName() != null && !event.getFullName().isBlank()) {
                user.setFullName(event.getFullName().trim());
            }

            userRepository.save(user);

            log.info(
                    "Đã sync KYC sang identity thành công: userId={}, email={}, fullName={}, role={}, kycStatus={}",
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole(),
                    user.getKycStatus()
            );

        } catch (Exception e) {
            log.error("Lỗi xử lý KYC event từ customer-service", e);
        }
    }
}