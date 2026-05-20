package com.homeverse.recommendation.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BanditArmStats {

    private String arm;

    private Double impressions;

    private Double rewards;

    private Double avgReward;

    private Double weight;
}