package com.homeverse.property.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SQLDelete;


@Entity
@Table(name = "properties")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE properties SET status = 'DELETED' WHERE id = ?")
@SQLRestriction("status <> 'DELETED'")
@DynamicUpdate
public class Property {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "is_quota_deducted", nullable = false)
    @Builder.Default
    private boolean isQuotaDeducted = false;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "project_name_snapshot")
    private String projectNameSnapshot;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private BigDecimal price;
    private Double area;


    private String address;


    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type")
    private PropertyType propertyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private TransactionType transactionType;

    private Integer capacity;


    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> images;

    @Column(name = "video_url")
    private String videoUrl;


    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "amenities", columnDefinition = "jsonb")
    private List<String> amenities;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "owner_name_snapshot")
    private String ownerNameSnapshot;

    @Column(name = "owner_avatar_snapshot")
    private String ownerAvatarSnapshot;


    @Column(name = "owner_slug_snapshot")
    private String ownerSlugSnapshot;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    @Column(name = "is_promoted", nullable = false)
    @Builder.Default
    private Boolean isPromoted = false; 

    @Column(name = "promotion_expires_at")
    private LocalDateTime promotionExpiresAt; 

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = Status.PENDING;
        if (expiresAt == null) expiresAt = createdAt.plusDays(30);
    }

    private Integer bedrooms;   // Số phòng ngủ
    private Integer bathrooms;  // Số phòng vệ sinh
    private Boolean hasBalcony; // Có ban công



    @Enumerated(EnumType.STRING)
    private FurnishingStatus furnishingStatus; // Tình trạng nội thất

    @Enumerated(EnumType.STRING)
    private AvailabilityStatus availabilityStatus; // Thời gian vào ở

    @Enumerated(EnumType.STRING)
    private UtilityPriceType electricityPrice; // Giá điện

    @Enumerated(EnumType.STRING)
    private UtilityPriceType waterPrice;       // Giá nước

    @Enumerated(EnumType.STRING)
    private UtilityPriceType internetPrice;    // Giá internet

    public enum PropertyType {
        APARTMENT,      // Căn hộ
        HOUSE,          // Nhà nguyên căn
        VILLA,          // Biệt thự
        COMMERCIAL,     // Mặt bằng kinh doanh
        ROOM            // Phòng trọ
    }

    public enum TransactionType {
        FOR_SALE,       // Bán
        FOR_RENT        // Cho thuê
    }
    public enum FurnishingStatus {
        UNFURNISHED,        // Nhà trống
        PARTIALLY_FURNISHED,// Nội thất cơ bản
        FULLY_FURNISHED     // Đầy đủ nội thất
    }
    public enum AvailabilityStatus {
        IMMEDIATELY,        // Vào ở ngay
        THIS_MONTH,         // Trong tháng này
        NEXT_MONTH,         // Đầu tháng sau
        NEGOTIABLE          // Thỏa thuận với chủ nhà
    }
    public enum UtilityPriceType {
        FREE,               // Miễn phí
        STATE_PRICE,        // Theo giá nhà nước / nhà cung cấp
        LANDLORD_PRICE,     // Theo quy định của chủ nhà (Khách tự hỏi)
        SHARED,             // Chia đều theo đầu người / phòng
        NEGOTIABLE          // Thỏa thuận
    }

    public enum Status { PENDING, ACTIVE, FULL, HIDDEN, EXPIRED, APPROVED, REJECTED, DELETED }
}