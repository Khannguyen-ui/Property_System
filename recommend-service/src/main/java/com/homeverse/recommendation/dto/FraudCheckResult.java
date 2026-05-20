package com.homeverse.recommendation.dto;

import com.homeverse.recommendation.enums.FraudDecision;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudCheckResult {

    private FraudDecision decision;

    private String reason;

    private Long currentCount;
}