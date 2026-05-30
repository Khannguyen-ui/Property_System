package com.homeverse.property.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotion_queue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id")
    private Long propertyId; // Dùng cho Room Promotion

    @Column(name = "user_id")
    private Long userId; // Dùng cho Membership

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "package_name")
    private String packageName;

    @Column(name = "priority_level")
    private Integer priorityLevel; // Gold = 3, Silver = 2, Basic = 1

    @Column(name = "duration_days")
    private Integer durationDays;
    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "transaction_id")
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PromotionStatus status; // WAITING, ACTIVE, EXPIRED, CANCELLED

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = PromotionStatus.WAITING;
        }
    }

    public enum PromotionStatus {
        WAITING, // Đang chờ đến lượt
        ACTIVE, // Đang có hiệu lực
        EXPIRED, // Đã hết hạn
        CANCELLED // Bị hủy (nếu Admin can thiệp)
    }
}