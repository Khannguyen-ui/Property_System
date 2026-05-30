package com.homeverse.property.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "owner_profiles")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OwnerProfile {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(unique = true, nullable = false)
    private String slug;
    private String phone;
    private String fullName;
    private String avatar;
}