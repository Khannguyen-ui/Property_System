package com.homeverse.chat.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

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

    @Field("is_read")
    private boolean isRead = false;

    @Field("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}