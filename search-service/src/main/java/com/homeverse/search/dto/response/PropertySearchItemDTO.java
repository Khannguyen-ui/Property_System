package com.homeverse.search.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PropertySearchItemDTO {
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
    private Boolean hasBalcony;
    private String furnishingStatus;

    private Double latitude;
    private Double longitude;

    private String thumbnail;
    private LocalDateTime createdAt;
}