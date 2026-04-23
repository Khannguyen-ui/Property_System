package com.homeverse.property.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PropertyResponseDTO {

    private Long id;
    private Long projectId;
    private String projectNameSnapshot;
    private String transactionType;
    private String title;
    private String description;
    private BigDecimal price;
    private BigDecimal pricePerSqm;

    private Double area;
    private String address;
    private String province;
    private String street;
    private String ward;
    private String district;

    private Double latitude;
    private Double longitude;

    private String propertyType;
    private Integer capacity;

    private List<String> images;
    private List<String> amenities;
    private String videoUrl;
    private boolean isQuotaDeducted;

    private String status;
    private Long ownerId;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    private Integer bedrooms;
    private Integer bathrooms;
    private Boolean hasBalcony;

    private String furnishingStatus;
    private String availabilityStatus;
    private String electricityPrice;
    private String waterPrice;
    private String internetPrice;
    private String legalDocumentType;
    private Long promotionPackageId;
    private String promotionPackageName;
    private Boolean isPromoted;
    private LocalDateTime promotionExpiresAt;
}