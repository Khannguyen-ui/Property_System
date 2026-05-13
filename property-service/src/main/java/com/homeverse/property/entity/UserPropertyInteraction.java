package com.homeverse.property.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_property_interactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_property_interaction",
                        columnNames = {"user_id", "property_id", "interaction_type"}
                ),
                @UniqueConstraint(
                        name = "uk_guest_property_interaction",
                        columnNames = {"guest_id", "property_id", "interaction_type"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPropertyInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = true)
    private Long userId;

    @Column(name = "guest_id", nullable = true)
    private String guestId;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false)
    private InteractionType interactionType;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum InteractionType {
        LIKE,
        SAVE
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}