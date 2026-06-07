package com.homeverse.search.controller;

import com.homeverse.search.dto.response.PropertyAnalyticsResponse;
import com.homeverse.search.dto.response.RegionTransactionStatDTO;
import com.homeverse.search.dto.response.WardPriceDTO;
import com.homeverse.search.service.PropertyAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class PropertyAnalyticsController {

    private final PropertyAnalyticsService analyticsService;

    @GetMapping("/price-trends")
    public ResponseEntity<PropertyAnalyticsResponse> getPriceTrends(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) String propertyType,
            @RequestParam String transactionType) {

        return ResponseEntity.ok(analyticsService.getPriceTrends(province, district, ward, propertyType, transactionType));
    }

    @GetMapping("/ward-prices")
    public ResponseEntity<List<WardPriceDTO>> getPricesByWards(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) String propertyType,
            @RequestParam String transactionType) {

        return ResponseEntity.ok(
                analyticsService.getPricesByWards(
                        province,
                        district,
                        ward,
                        propertyType,
                        transactionType
                )
        );
    }

    @GetMapping("/top-regions")
    public ResponseEntity<List<RegionTransactionStatDTO>> getTopRegionsTransactionStats(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "province.keyword") String regionField) {

        return ResponseEntity.ok(analyticsService.getTopRegionsTransactionStats(limit, regionField));
    }
}