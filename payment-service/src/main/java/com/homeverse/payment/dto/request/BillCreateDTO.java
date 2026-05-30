package com.homeverse.payment.dto.request;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillCreateDTO {
    private Long contractId;
    private int month;
    private int year;
    private int electricNew;
    private int waterNew;
    
    // Thêm các trường này để BillServiceImpl có cái mà "get"
    private BigDecimal electricPrice;
    private BigDecimal waterPrice;
    private BigDecimal monthlyRent;
    private BigDecimal serviceFees;
}