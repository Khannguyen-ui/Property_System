package com.homeverse.customer.kafka.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeverse.common.kafka.cdc.DebeziumMessage;
import com.homeverse.common.kafka.cdc.message.UserCdcMessage;
import com.homeverse.customer.entity.Customer;
import com.homeverse.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserSyncDataConsumer {

    private final ObjectMapper objectMapper;
    private final CustomerRepository customerRepository;

    @KafkaListener(topics = "identity_server.public.user_credentials", groupId = "customer-group")
    @Transactional
    public void consume(String message) {
        try {
            JsonNode rootNode = objectMapper.readTree(message);
            JsonNode payloadNode = rootNode.has("payload") ? rootNode.get("payload") : rootNode;

            DebeziumMessage<UserCdcMessage> debeziumMessage = objectMapper.convertValue(
                    payloadNode,
                    new TypeReference<DebeziumMessage<UserCdcMessage>>() {}
            );

            if (debeziumMessage == null || debeziumMessage.getOp() == null) return;

            String operation = debeziumMessage.getOp();
            UserCdcMessage payload = debeziumMessage.getAfter();


            if (("c".equals(operation) || "r".equals(operation)) && payload != null) {
                if (!customerRepository.existsById(payload.getId())) {


                    String generatedSlug = generateSlug(payload.getFullName());

                    Customer newCustomer = Customer.builder()
                            .id(payload.getId())
                            .publicId(generatedSlug)
                            .email(payload.getEmail())
                            .fullName(payload.getFullName())
                            .phone(payload.getPhone())
                            .kycStatus("UNVERIFIED")
                            .build();

                    customerRepository.save(newCustomer);
                    log.info("🎉 Đã tạo hồ sơ rỗng cho Customer ID: {}", payload.getId());
                }
            }

            if ("u".equals(operation) && payload != null) {
                customerRepository.findById(payload.getId()).ifPresent(customer -> {
                    boolean isChanged = false;

                    // Nếu User đổi Email bên Identity, đồng bộ sang đây
                    if (payload.getEmail() != null && !payload.getEmail().equals(customer.getEmail())) {
                        customer.setEmail(payload.getEmail());
                        isChanged = true;
                        log.info(" Đồng bộ đổi Email cho Customer ID: {}", payload.getId());
                    }

                    // Nếu Admin duyệt/từ chối KYC bằng tay bên Identity, đồng bộ trạng thái về đây
                    if (payload.getKycStatus() != null && !payload.getKycStatus().equals(customer.getKycStatus())) {
                        customer.setKycStatus(payload.getKycStatus());
                        isChanged = true;
                        log.info(" Đồng bộ KYC Status thành [{}] cho Customer ID: {}", payload.getKycStatus(), payload.getId());
                    }

                    if (isChanged) {
                        customerRepository.save(customer);
                    }
                });
            }

        } catch (Exception e) {
            log.error(" Lỗi khi xử lý thông điệp CDC: {}", e.getMessage(), e);
        }
    }


    private String generateSlug(String name) {
        if (name == null || name.trim().isEmpty()) {
            return UUID.randomUUID().toString().substring(0, 10);
        }
        String slug = name.replace("đ", "d").replace("Đ", "D");
        String normalized = Normalizer.normalize(slug, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        slug = pattern.matcher(normalized).replaceAll("");
        slug = slug.toLowerCase();
        slug = slug.replaceAll("[^a-z0-9]+", "-");
        slug = slug.replaceAll("^-|-$", "");
        String randomTail = UUID.randomUUID().toString().substring(0, 5);
        return slug + "-" + randomTail;
    }
}