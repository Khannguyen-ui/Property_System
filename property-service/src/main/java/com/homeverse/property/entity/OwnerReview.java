package com.homeverse.property.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "owner_reviews",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"owner_id", "reviewer_id", "property_id"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "property_id")
    private Long propertyId;

    @Column(nullable = false)
    private Integer rating; // 1 -> 5

    @Column(columnDefinition = "TEXT")
    private String comment;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}