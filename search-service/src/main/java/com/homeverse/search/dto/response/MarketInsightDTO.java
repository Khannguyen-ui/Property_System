package com.homeverse.search.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MarketInsightDTO {
    private String popularPriceText;
    private String popularPriceUnit;
    private String popularPriceLabel;

    private Double yearlyGrowthPercent;
    private String yearlyGrowthTrend;
    private String yearlyGrowthLabel;

    private Double diffFromPeakPercent;
    private String diffFromPeakTrend;
    private String diffFromPeakLabel;
}