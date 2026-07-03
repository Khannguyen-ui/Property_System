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

    private Double budget;
    private Double preferredArea;

    private String province;
    private String district;
    private String ward;
    private String street;

    private String propertyType;
    private String transactionType;

    private Integer bedrooms;
    private Integer bathrooms;
    private Boolean hasBalcony;

    private String furnishingStatus;
    private String availabilityStatus;

    @Column(columnDefinition = "TEXT")
    private String amenities;

    private LocalDateTime updatedAt;
}