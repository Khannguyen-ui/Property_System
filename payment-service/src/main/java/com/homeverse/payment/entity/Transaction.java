package com.homeverse.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter // Thêm cái này
@Setter // Thêm cái này
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sửa lại thành Long cho nhẹ đầu nếu chưa có class User
    @Column(name = "user_id")
    private Long userId; 

    private String description;
    private BigDecimal amount;
    private String type; 

    @Column(name = "vnpay_code")
    private String vnpayCode;

    @Column(name = "coupon_id")
    private Long couponId; // Sửa lại thành Long luôn

    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}