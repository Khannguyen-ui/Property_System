package com.homeverse.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Data
public class TrackEventRequest {

    private Long userId;

    private Long itemId;

    private String itemType;

    private String action;

    private Double watchTime;
    private Double duration;

    private Double price;

    private Double userBudget;

    private Integer categoryMatch;
    private String district;
    private Double area;
    private Double userArea;

    private Integer provinceMatch;
    private Integer districtMatch;
    private Integer wardMatch;
    private Integer streetMatch;
    private Integer locationMatch;

    private Integer transactionMatch;
    private String province;
    private String ward;
    private String street;
    private String propertyType;
    private String transactionType;
    private Integer bedroomMatch;
    private Integer bathroomMatch;
    private Integer balconyMatch;

    private Integer furnishingMatch;
    private Integer availabilityMatch;

    private Double amenityMatchRatio;
}