package com.homeverse.wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal; // Thêm import này
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 19, scale = 2) // Nên thêm precision để DB lưu chuẩn
    private BigDecimal amount; // Đã đổi sang BigDecimal

    @Column(name = "transaction_type", nullable = false, length = 30)
    private String transactionType; 

    @Column(name = "reference_id", length = 100)
    private String referenceId; 

    private String description;

    @Builder.Default // Dùng Builder thì nên có cái này để status không bị null
    @Column(length = 20)
    private String status = "COMPLETED";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}