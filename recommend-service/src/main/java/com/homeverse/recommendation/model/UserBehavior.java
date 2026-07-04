package com.homeverse.recommendation.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_behavior")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBehavior {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long itemId;

    private String itemType;

    private String action;

    private Double watchTime;

    private Double duration;

    // ===== Price =====
    private Double price;
    private Double userBudget;

    // ===== Area =====
    private Double area;
    private Double userArea;

    // ===== Feature Engineering =====
    private Integer provinceMatch;
    private Integer districtMatch;
    private Integer wardMatch;
    private Integer streetMatch;

    private Integer categoryMatch;
    private Integer transactionMatch;

    private Integer bedroomMatch;
    private Integer bathroomMatch;
    private Integer balconyMatch;

    private Integer furnishingMatch;
    private Integer availabilityMatch;

    private Double amenityMatchRatio;
    // ===== Property metadata =====

    private String province;

    private String district;

    private String ward;

    private String street;

    private String propertyType;

    private String transactionType;
    // ===== Fraud =====
    private Boolean suspicious;
    private String fraudReason;

    // ===== Final label =====
    private Double score;

    private LocalDateTime createdAt;
}