package com.homeverse.chat.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    private String id;

    @Field("conversation_id")
    private String conversationId;

    @Field("sender_id")
    private Long senderId;

    @Field("receiver_id")
    private Long receiverId;

    private String content;

    private String type;
    @Field("reply_to_message_id")
    private String replyToMessageId;

    @Field("reply_preview")
    private String replyPreview;

    @Field("reply_sender_id")
    private Long replySenderId;
    @Field("is_recalled")
    private boolean recalled = false;

    @Field("recalled_at")
    private LocalDateTime recalledAt;
    private String mediaUrl;
    @Field("reactions")
    private Map<Long, String> reactions = new HashMap<>();

    @Field("is_read")
    private boolean isRead = false;

    @Field("read_at")
    private LocalDateTime readAt;

    @Field("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}