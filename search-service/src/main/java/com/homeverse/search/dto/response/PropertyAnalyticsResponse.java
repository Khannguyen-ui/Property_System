package com.homeverse.search.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PropertyAnalyticsResponse {
    private MarketInsightDTO marketInsights;
    private List<PriceTrendDTO> trends;
}