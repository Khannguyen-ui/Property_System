package com.homeverse.wallet.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeverse.common.dto.RefundEvent;
import com.homeverse.wallet.entity.Wallet;
import com.homeverse.wallet.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundConsumer {

    private final WalletRepository walletRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "refund-payment-topic", groupId = "wallet-refund-group")
    @Transactional
    public void consumeRefund(String message) {
        log.info("💰 [WALLET-SERVICE] Nhận tin nhắn hoàn tiền: {}", message);

        try {
            RefundEvent event = objectMapper.readValue(message, RefundEvent.class);

            Wallet wallet = walletRepository.findByUserId(event.getUserId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ví cho User ID: " + event.getUserId()));

            BigDecimal refundAmount = event.getAmount();
            BigDecimal currentBalance = wallet.getBalance();
            
            // Thực hiện cộng tiền (BigDecimal cộng an toàn bằng phương thức add)
            BigDecimal newBalance = currentBalance.add(refundAmount);
            wallet.setBalance(newBalance);

            walletRepository.save(wallet);

            log.info("✅ [REFUND-SUCCESS] Đã hoàn {} vào ví User {}. Số dư mới: {}. Lý do: {}", 
                    refundAmount, event.getUserId(), newBalance, event.getReason());

        } catch (Exception e) {
            log.error("💥 [REFUND-ERROR] Lỗi xử lý hoàn tiền: {}", e.getMessage());
            throw new RuntimeException("Thất bại khi xử lý hoàn tiền Kafka", e);
        }
    }
}