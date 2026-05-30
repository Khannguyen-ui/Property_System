package com.homeverse.aiworker.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyEventDTO implements Serializable {
    private Long propertyId;
    private String status;
    private String title;
    private String description;

    // Địa chỉ chi tiết
    private String address;
    private String district;
    private String street;
    private String ward;
    private String province;

    // Giá & Pháp lý
    private Double price;
    private Double pricePerSqm;
    private String legalDocumentType;

    // Cấu trúc & Loại hình
    private Double area;
    private String propertyType;
    private String transactionType;
    private Integer bedrooms;
    private Integer bathrooms;
    private Boolean hasBalcony;
    private Integer capacity; // Sức chứa

    // Tình trạng & Tiện ích
    private String furnishingStatus;
    private String availabilityStatus;
    private List<String> amenities;

    // Chi phí sinh hoạt
    private String electricityPrice;
    private String waterPrice;
    private String internetPrice;

    private List<String> images;

    private String eventType; // "UPSERT" hoặc "DELETE"
}