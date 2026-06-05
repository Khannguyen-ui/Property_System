package com.homeverse.recommendation.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
public class PropertyReelResponseDTO {

    private Long id;

    private String title;

    private BigDecimal price;

    private String address;

    private String videoUrl;

    private Long viewCount;
    private Long likeCount;
    private Long saveCount;
    private Boolean isLiked;
    private Boolean isSaved;
    private Long commentCount;
    private String ownerSlug;
    private Long ownerId;

    private String ownerNameSnapshot;

    private String ownerAvatarSnapshot;

    private LocalDateTime createdAt;

    private Boolean isPromoted;
    private String itemType;
    private Double score;
    private List<String> reasons;
    private Double ownerTrustScore;
}