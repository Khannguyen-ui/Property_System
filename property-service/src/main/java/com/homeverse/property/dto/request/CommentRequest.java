package com.homeverse.property.dto.request;

import lombok.Data;

@Data
public class CommentRequest {
    private Long propertyId;
    private Long parentId;
    private Long replyToUserId;
    private String content;
}