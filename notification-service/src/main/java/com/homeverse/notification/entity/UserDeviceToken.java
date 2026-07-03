package com.homeverse.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_device_tokens",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"userId", "token"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(length = 600, nullable = false)
    private String token;

    private String platform; // ANDROID, IOS

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}