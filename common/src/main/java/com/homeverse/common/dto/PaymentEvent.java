package com.homeverse.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentEvent {
    private Long userId;
    private BigDecimal amount;
    private Long packageId;
    private String packageName;
    private String transactionId;
    private Long roomId; 
    private String status;
    private String type;
    private Integer durationDays;
    private Integer priorityLevel;
}