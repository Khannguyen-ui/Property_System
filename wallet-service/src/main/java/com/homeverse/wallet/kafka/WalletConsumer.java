package com.homeverse.wallet.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeverse.common.dto.PaymentEvent;
import com.homeverse.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletConsumer {

    private final ObjectMapper objectMapper;
    private final WalletService walletService;

   @KafkaListener(topics = "payment-success-topic", groupId = "wallet-group-v2")
public void consumePaymentSuccess(String message) {
    try {
        // Log này để xác nhận ĐÚNG LÀ CODE MỚI ĐANG CHẠY
        log.info(">>> PHIÊN BẢN MỚI NHẤT - Nhận tin: {}", message);
        
        PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);
        String type = event.getType();

        // Kiểm tra điều kiện: CHỈ nạp tiền nếu KHÔNG PHẢI membership/promotion
        if (type != null && (type.equalsIgnoreCase("MEMBERSHIP") || type.equalsIgnoreCase("ROOM_PROMOTION"))) {
            log.info(">>> ĐÃ CHẶN: Giao dịch loại {} không được cộng tiền.", type);
            return; 
        }

        if (event.getUserId() != null && event.getAmount() != null) {
            walletService.handleTransaction(
                event.getUserId(),
                event.getAmount(),
                type != null ? type : "DEPOSIT",
                event.getTransactionId(),
                "Nạp tiền hệ thống"
            );
        }
    } catch (Exception e) {
        log.error("Lỗi Kafka: {}", e.getMessage());
    }
}
}