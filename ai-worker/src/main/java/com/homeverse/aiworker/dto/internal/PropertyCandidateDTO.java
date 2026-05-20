package com.homeverse.aiworker.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyCandidateDTO {

    private Long propertyId;

    private String title;

    private Double price;
    private Double pricePerSqm;

    private String province;
    private String district;
    private String ward;
    private String street;
    private String address;

    private String propertyType;
    private String transactionType;
    private String status;

    private Double area;
    private Integer bedrooms;
    private Integer bathrooms;
    private Integer capacity;
    private Boolean hasBalcony;

    private String furnishingStatus;
    private String availabilityStatus;
    private String legalDocumentType;

    private String electricityPrice;
    private String waterPrice;
    private String internetPrice;

    private List<String> amenities;

    private String imageUrl;
}