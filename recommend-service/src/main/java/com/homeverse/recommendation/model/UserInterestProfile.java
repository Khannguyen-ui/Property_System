package com.homeverse.recommendation.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_interest_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInterestProfile {

    @Id
    private Long userId;

    private String favoriteItemType;

    private String favoriteAction;

    private Double avgScore;

    private Double avgBudget;

    private LocalDateTime updatedAt;
}