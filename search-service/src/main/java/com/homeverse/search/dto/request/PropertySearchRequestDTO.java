package com.homeverse.search.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PropertySearchRequestDTO {
    private String keyword;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Double minArea;
    private Double maxArea;
    private List<String> propertyTypes;
    private List<String> transactionTypes;
    private List<String> amenities;

    private String district;
    private String ward;
    private String street;
    private String province;
    private Double latitude;
    private Double longitude;
    private Integer radiusKm;

    // --- LỌC NÂNG CAO ---
    private Integer minBedrooms;
    private Integer minBathrooms;
    private Boolean hasBalcony;
    private Integer minCapacity;
    private Long projectId;

    private List<String> furnishingStatuses;
    private List<String> availabilityStatuses;

    // <--  TRƯỜNG GIÁ DỊCH VỤ -->
    private List<String> electricityPrices;
    private List<String> waterPrices;
    private List<String> internetPrices;
    private String filterMonth;

    private String sortBy = "createdAt";
    private String sortDir = "desc";
    private int page = 0;
    private int size = 12;
}