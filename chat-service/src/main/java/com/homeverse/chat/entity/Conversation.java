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

    @Field("updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}