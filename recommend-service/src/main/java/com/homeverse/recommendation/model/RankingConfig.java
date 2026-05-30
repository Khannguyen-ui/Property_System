package com.homeverse.recommendation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "ranking_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingConfig {

    @Id
    private String id;

    private Double promotedBoost;

    private Double districtBoost;

    private Double favoriteTypeBoost;

    private Double favoriteActionBoost;

    private Double ownerTrustWeight;

    private Double freshOneHourBoost;

    private Double freshOneDayBoost;

    private Double freshThreeDaysBoost;

    private Integer promotedLimit;

    private Integer behaviorLimit;

    private Integer collaborativeLimit;

    private Integer trendingLimit;

    private Integer randomLimit;

    private Integer finalLimit;
}