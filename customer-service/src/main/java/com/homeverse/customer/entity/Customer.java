package com.homeverse.customer.entity;

import com.homeverse.common.entity.BaseAuditEntity;
import com.homeverse.customer.model.json.LifestyleProfile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "user_credentials")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Customer extends BaseAuditEntity {

    @Id
    private Long id;

    @Column(unique = true, updatable = false, nullable = false)
    private String publicId;

    @Column(unique = true, nullable = false)
    private String email;

    private String fullName;
    private String phone;
    private String address;
    private String avatarUrl;
    private String bannerUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private LifestyleProfile lifestyleProfile;


    private String citizenId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> citizenImages;

    private String kycStatus;

    @PrePersist
    protected void onCreateEntity() {
        if (kycStatus == null) kycStatus = "UNVERIFIED";
    }

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}