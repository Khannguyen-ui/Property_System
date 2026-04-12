package com.homeverse.property.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_property_interactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPropertyInteraction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private Long userId;

    @Column(nullable = true)
    private String guestId;

    private Long propertyId;

    @Enumerated(EnumType.STRING)
    private InteractionType interactionType;

    private LocalDateTime createdAt;

    public enum InteractionType { LIKE, SAVE }

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}