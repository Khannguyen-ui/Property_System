package com.homeverse.property.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class UserInterestProfileDTO {

    private Double budget;

    private Double preferredArea;

    private String province;

    private String district;
    private String street;

    private String ward;

    private String propertyType;

    private String transactionType;

    private Integer bedrooms;

    private Integer bathrooms;

    private Boolean hasBalcony;

    private String furnishingStatus;

    private String availabilityStatus;

    private List<String> amenities;

}
