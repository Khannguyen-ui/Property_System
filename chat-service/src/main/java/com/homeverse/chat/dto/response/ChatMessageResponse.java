package com.homeverse.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    // Sửa Long thành String để khớp với ObjectId của MongoDB
    private String id;

    private Long senderId;
    private Long receiverId;
    private String content;
    private String type;
    private String replyToMessageId;
    private String mediaUrl;
    private Map<Long, String> reactions;
    private String replyPreview;
    private boolean recalled;
    private LocalDateTime recalledAt;
    private Long replySenderId;
    private LocalDateTime createdAt;
}