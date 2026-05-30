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

import javax.lang.model.element.Name;

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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(name = "price_per_sqm", precision = 19, scale = 2)
    private BigDecimal pricePerSqm;
    private Double area;

    private String address;
    @Column(name = "province")
    private String province;

    @Column(name = "district")
    private String district;

    @Column(name = "ward")
    private String ward;

    @Column(name = "street")
    private String street;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type")
    private PropertyType propertyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private TransactionType transactionType;
    @Enumerated(EnumType.STRING)
    @Column(name = "legal_document_type")
    private LegalDocumentType legalDocumentType = LegalDocumentType.NONE;

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

    @Column(name = "owner_phone_snapshot")
    private String ownerPhoneSnapshot;

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
    @Column(name = "promotion_package_id")
    private Long promotionPackageId;
    @Column(name = "promotion_package_name")
    private String promotionPackageName;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null)
            status = Status.PENDING;
        if (expiresAt == null)
            expiresAt = createdAt.plusDays(30);
        calculatePricePerSqm();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculatePricePerSqm(); // Tự tính lại nếu chủ nhà đổi giá/diện tích
    }

    private void calculatePricePerSqm() {
        if (this.price != null && this.area != null && this.area > 0) {

            this.pricePerSqm = this.price.divide(BigDecimal.valueOf(this.area), 0, java.math.RoundingMode.HALF_UP);
        } else {
            this.pricePerSqm = BigDecimal.ZERO;
        }
    }

    private Integer bedrooms; // Số phòng ngủ
    private Integer bathrooms; // Số phòng vệ sinh
    private Boolean hasBalcony; // Có ban công

    @Enumerated(EnumType.STRING)
    private FurnishingStatus furnishingStatus; // Tình trạng nội thất

    @Enumerated(EnumType.STRING)
    private AvailabilityStatus availabilityStatus; // Thời gian vào ở

    @Enumerated(EnumType.STRING)
    private UtilityPriceType electricityPrice; // Giá điện

    @Enumerated(EnumType.STRING)
    private UtilityPriceType waterPrice; // Giá nước

    @Enumerated(EnumType.STRING)
    private UtilityPriceType internetPrice; // Giá internet

    public enum PropertyType {
        APARTMENT, // Căn hộ
        HOUSE, // Nhà nguyên căn
        VILLA, // Biệt thự
        COMMERCIAL, // Mặt bằng kinh doanh
        ROOM // Phòng trọ
    }

    public enum TransactionType {
        FOR_SALE, // Bán
        FOR_RENT // Cho thuê
    }

    public enum FurnishingStatus {
        UNFURNISHED, // Nhà trống
        PARTIALLY_FURNISHED, // Nội thất cơ bản
        FULLY_FURNISHED // Đầy đủ nội thất
    }

    public enum AvailabilityStatus {
        IMMEDIATELY, // Vào ở ngay
        THIS_MONTH, // Trong tháng này
        NEXT_MONTH, // Đầu tháng sau
        NEGOTIABLE // Thỏa thuận với chủ nhà
    }

    public enum UtilityPriceType {
        FREE, // Miễn phí
        STATE_PRICE, // Theo giá nhà nước / nhà cung cấp
        LANDLORD_PRICE, // Theo quy định của chủ nhà (Khách tự hỏi)
        SHARED, // Chia đều theo đầu người / phòng
        NEGOTIABLE // Thỏa thuận
    }

    public enum LegalDocumentType {
        NONE, // Không cung cấp
        CERTIFICATE_OF_OWNERSHIP, // Sổ đỏ / Sổ hồng (Chính chủ)
        LEASE_CONTRACT, // Hợp đồng thuê nhà (Cho thuê lại)
        AUTHORIZATION_LETTER // Giấy ủy quyền
    }

    public enum Status {
        PENDING, ACTIVE, FULL, HIDDEN, EXPIRED, APPROVED, REJECTED, DELETED
    }
}