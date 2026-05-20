package com.homeverse.aiworker.dto.search;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SearchPropertyItemDTO {
    private Long id;

    private String propertyType;
    private String transactionType;

    private String title;
    private BigDecimal price;
    private Double area;

    private String address;
    private String province;
    private String street;
    private String ward;
    private String district;

    private Integer bedrooms;
    private Integer bathrooms;
    private Integer capacity;
    private Boolean hasBalcony;

    private String furnishingStatus;
    private String availabilityStatus;

    private String electricityPrice;
    private String waterPrice;
    private String internetPrice;

    private List<String> amenities;

    private String thumbnail;
    private LocalDateTime createdAt;
}