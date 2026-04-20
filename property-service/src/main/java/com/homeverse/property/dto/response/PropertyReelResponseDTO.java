package com.homeverse.property.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PropertyReelResponseDTO {
    private Long id;
    private String title;
    private BigDecimal price;
    private String address;
    private String videoUrl;

    private boolean isLiked;
    private boolean isSaved;
    private Long likeCount;
    private String ownerSlug;
    private String ownerNameSnapshot;
    private String ownerAvatarSnapshot;

    private LocalDateTime createdAt;
}