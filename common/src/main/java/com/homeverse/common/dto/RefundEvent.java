package com.homeverse.common.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefundEvent {
    private Long userId;
    private BigDecimal amount;
    private String transactionId; // ID giao dịch gốc để đối soát
    private String reason;       // Lý do hoàn tiền (Admin Rejected)
}
