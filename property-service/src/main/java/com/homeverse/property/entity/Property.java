package com.homeverse.property.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point; // Yêu cầu thư viện hibernate-spatial

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "properties")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- CẢI TIẾN MICROSERVICES: Lưu ID thay vì Object ---
    @Column(name = "landlord_id", nullable = false)
    private Long landlordId;

    @Column(name = "service_package_id")
    private Long servicePackageId;

    // ---------------------------------------------------

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "search_vector", columnDefinition = "tsvector", insertable = false, updatable = false)
    private String searchVector;

    private BigDecimal price; // Giá hiển thị
    private BigDecimal deposit; // Tiền cọc

    private Double area;      // Diện tích

    // Tách địa chỉ rõ ràng để dễ filter
    private String province;
    private String district;
    private String ward;
    private String addressDetail;

    @Column(name = "auto_renew")// Gia hạn tin
    private Boolean autoRenew = false;

    @Column(name = "last_pushed_at")
    private LocalDateTime lastPushedAt; // Thời gian đẩy tin

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "furniture_status")
    private String furnitureStatus; // VD: "Nội thất đầy đủ", "Cơ bản", "Nhà trống"

    @Column(name = "package_type")
    private String packageType;    // Lưu tên gói: "Hội viên Vàng", "VIP"...

    @Column(name = "priority_level")
    private Integer priorityLevel;

    @Column(name = "legal_status")
    private String legalStatus; // VD: "Đã có sổ", "Đang chờ sổ"

    private String direction; // VD: "Đông Nam", "Tây Bắc"

    @Column(name = "floor_number")
    private Integer floorNumber; // Tầng mấy

    @Column(name = "num_bedrooms")
    private Integer numBedrooms;

    @Column(name = "num_bathrooms")
    private Integer numBathrooms;

    // --- CORE HYBRID LOGIC ---
    @Enumerated(EnumType.STRING)
    private RentalType rentalType; // WHOLE (Nguyên căn) - SHARED (Ở ghép)

    private Integer capacity; // Tổng chỗ (VD: 1 hoặc 8)

    @Builder.Default
    @Column(name = "current_tenants")
    private Integer currentTenants = 0; // Số người đang ở

    @Enumerated(EnumType.STRING)
    private GenderConstraint genderConstraint; // MALE_ONLY, FEMALE_ONLY, MIXED

    // --- GIS & MEDIA ---
    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point location; // Tọa độ bản đồ Google Maps

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> images;

    @Column(name = "video_url", columnDefinition = "TEXT")
    private String videoUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> amenities;

    // --- TRẠNG THÁI & THỐNG KÊ ---
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate; // Ngày hết hạn tin đăng

    @Column(name = "promotion_expiration")
    private LocalDateTime promotionExpiration;

    @Builder.Default
    @Column(name = "average_rating")
    private Double averageRating = 0.0;

    @Builder.Default
    @Column(name = "total_reviews")
    private Integer totalReviews = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = Status.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // --- CÁC ENUM ---
    public enum RentalType { WHOLE, SHARED }
    public enum GenderConstraint { MALE_ONLY, FEMALE_ONLY, MIXED }
    public enum Status { PENDING, ACTIVE, FULL, HIDDEN, EXPIRED, APPROVED, REJECTED }
}