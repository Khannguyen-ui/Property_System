package com.homeverse.chat.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "conversations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    private String id;

    @Field("user1_id")
    private Long user1Id;

    @Field("user2_id")
    private Long user2Id;

    @Field("last_message")
    private String lastMessage;

    @Field("last_message_sender_id")
    private Long lastMessageSenderId;

    @Field("user1_unread")
    @Builder.Default
    private Integer user1Unread = 0;

    @Field("user2_unread")
    @Builder.Default
    private Integer user2Unread = 0;

    @Field("user1_last_read_at")
    private LocalDateTime user1LastReadAt;

    @Field("user2_last_read_at")
    private LocalDateTime user2LastReadAt;

    @Field("updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}