package com.homeverse.property.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "owner_quotas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerQuota {

    @Id
    private Long ownerId;

    private Integer freePostsRemaining;
    private String role;
}