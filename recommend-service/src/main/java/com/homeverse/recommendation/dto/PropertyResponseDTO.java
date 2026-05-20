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
    private LocalDateTime createdAt;
    private Boolean isPromoted;
    private String itemType;
    private Double score;
    private String district;
    private Long ownerId;
    private List<String> reasons;
    private Double ownerTrustScore;
}