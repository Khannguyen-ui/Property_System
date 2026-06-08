package com.homeverse.recommendation.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsDashboardResponse {

    private Long totalBehaviors;

    private Long totalViews;

    private Long totalClicks;

    private Long totalLikes;

    private Long totalSaves;

    private Long totalContacts;

    private Double avgScore;

    private String topItemType;

    private String topAction;
    private Double ctr;

    private Double contactRate;
    private Double likeRate;

    private Double saveRate;
}