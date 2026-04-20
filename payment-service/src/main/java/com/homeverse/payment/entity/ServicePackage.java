package com.homeverse.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServicePackage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Đổi thành Long để khớp với Repository

    private String name;
    private BigDecimal price;
    private Integer durationDays;

    private Double discountPercent;

    // Thêm trường này để hết lỗi setPriorityLevel
    private Integer priorityLevel;

    @Enumerated(EnumType.STRING)
    private PackageType type; // MEMBERSHIP hoặc ROOM_PROMOTION

    private String description;

    @Builder.Default
    private Boolean active = true;
}