package com.homeverse.identity.kafka.consumer; // 🚨 Package chuẩn

import com.homeverse.identity.entity.UserCredential;
import com.homeverse.identity.repository.UserCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component // Dùng @Component hoặc @Service đều được để Spring quét
@RequiredArgsConstructor
public class KycEventConsumer {

    private final UserCredentialRepository userRepository;


    @KafkaListener(topics = "kyc-approved-topic", groupId = "identity-group")
    @Transactional
    public void handleKycApprovedEvent(String email) {
        log.info(" Bắt được sự kiện duyệt KYC tự động cho email: {}", email);

        try {
            UserCredential user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy User với email: " + email));


            user.setRole(UserCredential.Role.OWNER);
            user.setKycStatus("VERIFIED");

            userRepository.save(user);

            log.info(" Đã nâng Role lên OWNER thành công cho: {}", email);
        } catch (Exception e) {
            log.error("Lỗi khi xử lý nâng Role tự động: {}", e.getMessage());
        }
    }
}