package com.homeverse.search.service;

import com.homeverse.search.dto.response.PriceTrendDTO;
import com.homeverse.search.dto.response.PropertyAnalyticsResponse;
import com.homeverse.search.dto.response.RegionTransactionStatDTO;
import com.homeverse.search.dto.response.WardPriceDTO;

import java.util.List;

public interface PropertyAnalyticsService {
    PropertyAnalyticsResponse getPriceTrends(String province, String district, String ward, String propertyType, String transactionType);

    List<WardPriceDTO> getPricesByWards(String province, String district, String propertyType, String transactionType);

    List<RegionTransactionStatDTO> getTopRegionsTransactionStats(int topK, String regionField);
}