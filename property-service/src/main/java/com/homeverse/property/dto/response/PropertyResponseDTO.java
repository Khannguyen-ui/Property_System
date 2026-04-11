package com.homeverse.property.dto.response;

import com.homeverse.property.entity.Property;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PropertyResponseDTO {
    private Long id;
    private String title;
    private String description;

    private BigDecimal price;
    private BigDecimal deposit;
    private Double area;

    private String province;
    private String district;
    private String ward;
    private String addressDetail;

    // Bóc tách Point thành 2 biến cho Frontend dễ vẽ Map
    private Double latitude;
    private Double longitude;

    private String furnitureStatus;
    private String legalStatus;
    private String direction;
    private Integer floorNumber;
    private Integer numBedrooms;
    private Integer numBathrooms;

    private Property.RentalType rentalType;
    private Integer capacity;
    private Integer currentTenants;
    private Property.GenderConstraint genderConstraint;

    private List<String> images;
    private String videoUrl;
    private List<String> amenities;

    private Property.Status status;
    private Double averageRating;
    private Integer totalReviews;
    private Boolean autoRenew;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Object thông tin Chủ trọ (lấy từ identity-service)
    private UserResponseDTO landlordInfo;
}