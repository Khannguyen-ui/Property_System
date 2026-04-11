package com.homeverse.payment.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder // <--- Quan trọng để dùng được .builder()
@NoArgsConstructor
@AllArgsConstructor
public class BillResponseDTO {
    private Long id;
    private BigDecimal totalAmount;
    private String status;
}