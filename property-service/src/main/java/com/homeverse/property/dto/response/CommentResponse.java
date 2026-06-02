package com.homeverse.property.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponse {
    private Long id;
    private Long propertyId;
    private Long userId;
    private String guestId;
    private Long parentId;
    private Long replyToUserId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}