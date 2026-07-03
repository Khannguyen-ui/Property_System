package com.homeverse.recommendation.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
public class PropertyResponseDTO {
    private Long id;
    private String title;
    private BigDecimal price;
    private String address;
    private String videoUrl;
    private List<String> images;
    private LocalDateTime createdAt;
    private Boolean isPromoted;
    private String itemType;
    private Double score;
    private String district;
    private Long ownerId;
    private List<String> reasons;
    private Double area;

    private String province;
    private String ward;
    private String street;

    private String propertyType;
    private String transactionType;

    private Integer bedrooms;
    private Integer bathrooms;

    private Boolean hasBalcony;

    private String furnishingStatus;
    private String availabilityStatus;

    private List<String> amenities;
    private Double ownerTrustScore;
    private Long viewCount;
    private Long likeCount;
    private Long saveCount;
    private Boolean isLiked;
    private Boolean isSaved;
    private Long commentCount;
    private Long contactCount;
    private String primarySource;
    private Long shareCount;
}