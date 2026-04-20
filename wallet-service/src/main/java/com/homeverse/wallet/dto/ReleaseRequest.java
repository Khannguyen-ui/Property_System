package com.homeverse.wallet.dto;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReleaseRequest {
    private Long userId; // Đổi từ UUID sang Long ở đây luôn
    private BigDecimal amount;
    private String referenceId;
}