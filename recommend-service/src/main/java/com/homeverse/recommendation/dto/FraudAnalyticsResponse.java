package com.homeverse.recommendation.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAnalyticsResponse {

    private Long blockedSpamLastMinute;

    private Boolean suspicious;

    private String suspiciousReason;
}