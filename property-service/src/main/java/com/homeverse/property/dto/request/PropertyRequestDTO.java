package com.homeverse.property.dto.request;

import com.homeverse.property.entity.Property;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PropertyRequestDTO {
    private String title;
    private String description;

    private BigDecimal price;
    private BigDecimal deposit;
    private Double area;

    private String province;
    private String district;
    private String ward;
    private String addressDetail;

    // Tọa độ để Map thành Point
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
    private Property.GenderConstraint genderConstraint;

    private List<String> images;
    private String videoUrl;
    private List<String> amenities;

    // (Tùy chọn) ID gói cước nếu có tích hợp thanh toán luôn
    private Long servicePackageId;
}