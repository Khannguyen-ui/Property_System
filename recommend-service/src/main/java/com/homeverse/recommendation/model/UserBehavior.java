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

    private Double price;
    private Boolean suspicious;

    private String fraudReason;

    private Double userBudget;

    private Integer locationMatch;

    private Integer categoryMatch;

    private Double score;

    private LocalDateTime createdAt;

}