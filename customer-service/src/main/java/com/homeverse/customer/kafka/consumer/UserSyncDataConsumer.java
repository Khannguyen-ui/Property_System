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

    @KafkaListener(topics = "identity_server.public.user_credentials", groupId = "customer-group-v5")
    @Transactional
    public void consume(String message) {
        try {
            JsonNode rootNode = objectMapper.readTree(message);
            JsonNode payloadNode = rootNode.has("payload") ? rootNode.get("payload") : rootNode;

            DebeziumMessage<UserCdcMessage> debeziumMessage = objectMapper.convertValue(
                    payloadNode,
                    new TypeReference<DebeziumMessage<UserCdcMessage>>() {
                    }
            );

            if (debeziumMessage == null || debeziumMessage.getOp() == null) {
                return;
            }

            String operation = debeziumMessage.getOp();
            UserCdcMessage after = debeziumMessage.getAfter();
            UserCdcMessage before = debeziumMessage.getBefore();

            if ("d".equals(operation)) {
                handleDelete(before);
                return;
            }

            if (("c".equals(operation) || "r".equals(operation) || "u".equals(operation)) && after != null) {
                syncCustomer(after);
            }

        } catch (Exception e) {
            log.error("Lỗi khi xử lý thông điệp CDC Identity -> Customer: {}", e.getMessage(), e);
        }
    }

    private void handleDelete(UserCdcMessage before) {
        if (before == null || before.getId() == null) {
            log.warn("CDC delete nhưng before/id rỗng, bỏ qua");
            return;
        }

        if (customerRepository.existsById(before.getId())) {
            customerRepository.deleteById(before.getId());
            log.info("Đã xóa Customer theo Identity ID: {}", before.getId());
        } else {
            log.info("Không tìm thấy Customer ID {} để xóa, bỏ qua", before.getId());
        }
    }

    private void syncCustomer(UserCdcMessage payload) {
        if (payload.getId() == null) {
            log.warn("CDC user không có id, bỏ qua");
            return;
        }

        customerRepository.findById(payload.getId()).ifPresentOrElse(customer -> {
            boolean changed = false;

            if (payload.getEmail() != null && !payload.getEmail().equals(customer.getEmail())) {
                customer.setEmail(payload.getEmail());
                changed = true;
                log.info("Đồng bộ đổi Email cho Customer ID: {}", payload.getId());
            }

            if (payload.getFullName() != null && !payload.getFullName().equals(customer.getFullName())) {
                customer.setFullName(payload.getFullName());
                changed = true;
                log.info("Đồng bộ FullName cho Customer ID: {}", payload.getId());
            }

            if (payload.getKycStatus() != null && !payload.getKycStatus().equals(customer.getKycStatus())) {
                customer.setKycStatus(payload.getKycStatus());
                changed = true;
                log.info("Đồng bộ KYC Status [{}] cho Customer ID: {}", payload.getKycStatus(), payload.getId());
            }

            if (changed) {
                customerRepository.save(customer);
            }
        }, () -> {
            if (payload.getEmail() != null) {
                customerRepository.findByEmail(payload.getEmail()).ifPresentOrElse(existing -> {
                    Long oldId = existing.getId();

                    if (!oldId.equals(payload.getId())) {
                        if (customerRepository.existsById(payload.getId())) {
                            log.error(
                                    "Không thể merge Customer email {} từ ID {} sang ID {} vì ID mới đã tồn tại",
                                    payload.getEmail(),
                                    oldId,
                                    payload.getId()
                            );
                            return;
                        }

                        customerRepository.updateCustomerId(oldId, payload.getId());
                        log.info(
                                "Đã sửa lệch Customer theo email {}: ID {} -> {}",
                                payload.getEmail(),
                                oldId,
                                payload.getId()
                        );

                        customerRepository.findById(payload.getId()).ifPresent(updated -> {
                            boolean changed = false;

                            if (payload.getFullName() != null && !payload.getFullName().equals(updated.getFullName())) {
                                updated.setFullName(payload.getFullName());
                                changed = true;
                            }

                            if (payload.getKycStatus() != null && !payload.getKycStatus().equals(updated.getKycStatus())) {
                                updated.setKycStatus(payload.getKycStatus());
                                changed = true;
                            }

                            if (changed) {
                                customerRepository.save(updated);
                            }
                        });
                    }
                }, () -> createCustomer(payload));
            } else {
                createCustomer(payload);
            }
        });
    }

    private void createCustomer(UserCdcMessage payload) {
        String generatedSlug = generateSlug(payload.getFullName());

        Customer newCustomer = Customer.builder()
                .id(payload.getId())
                .publicId(generatedSlug)
                .email(payload.getEmail())
                .fullName(payload.getFullName())
                .kycStatus(payload.getKycStatus() != null ? payload.getKycStatus() : "UNVERIFIED")
                .build();

        customerRepository.save(newCustomer);
        log.info("Đã tạo hồ sơ rỗng cho Customer ID: {}", payload.getId());
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