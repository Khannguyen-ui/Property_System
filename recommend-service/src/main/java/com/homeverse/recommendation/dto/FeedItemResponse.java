package com.homeverse.recommendation.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedItemResponse {

    private Long itemId;

    private String itemType;

    private String title;

    private BigDecimal price;

    private String address;

    private String videoUrl;

    private Boolean isLiked;

    private Boolean isSaved;

    private Long likeCount;

    private String ownerSlug;

    private String ownerNameSnapshot;

    private String ownerAvatarSnapshot;

    private LocalDateTime createdAt;

    private Boolean isPromoted;

    private Double score;
}