package com.homeverse.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String code; // VD: GIAM50K

    @Column(name = "discount_value", nullable = false)
    private BigDecimal discountValue;

    @Column(name = "discount_type")
    private String discountType; // AMOUNT (Tiền) hoặc PERCENT (%)

    private Integer quantity; // Số lượng mã còn lại

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;
}