package com.homeverse.property.repository;

import com.homeverse.property.entity.Property;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findByStatus(Property.Status status);
    Page<Property> findByStatus(Property.Status status, Pageable pageable);
    List<Property> findByLandlordId(Long landlordId);
    Page<Property> findByLandlordId(Long landlordId, Pageable pageable);
    Optional<Property> findByIdAndLandlordId(Long id, Long landlordId);
    int countByLandlordId(Long landlordId);

    // 1. TÌM KIẾM QUANH ĐÂY (Giữ nguyên logic cực xịn của bạn)
    @Query(value = "SELECT p.* FROM properties p " +
            "WHERE p.status = 'ACTIVE' " +
            "AND ST_DWithin(p.location\\:\\:geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)\\:\\:geography, :radius) " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "p.search_vector @@ plainto_tsquery('simple', lower(:keyword))) " +
            "ORDER BY (CASE WHEN p.expiration_date >= NOW() THEN COALESCE(p.priority_level, 0) ELSE 0 END) DESC, " +
            "p.created_at DESC",
            countQuery = "SELECT count(*) FROM properties p " +
                    "WHERE p.status = 'ACTIVE' " +
                    "AND ST_DWithin(p.location\\:\\:geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)\\:\\:geography, :radius) " +
                    "AND (:keyword IS NULL OR :keyword = '' OR " +
                    "p.search_vector @@ plainto_tsquery('simple', lower(:keyword)))",
            nativeQuery = true)
    Page<Property> findPropertiesNearby(@Param("latitude") double latitude,
                                        @Param("longitude") double longitude,
                                        @Param("radius") double radiusInMeters,
                                        @Param("keyword") String keyword,
                                        Pageable pageable);

    // 2. TÍNH TOÁN THỐNG KÊ GIÁ (Dành cho Background Job)
    @Query(value = "SELECT " +
            "  CAST(MIN(price) AS NUMERIC) as min_p, " +
            "  CAST(AVG(price) AS NUMERIC) as avg_p, " +
            "  CAST(MAX(price) AS NUMERIC) as max_p " +
            "FROM properties " +
            "WHERE status = 'ACTIVE' " +
            "AND rental_type = :rentalType " +
            "AND ST_DistanceSphere(location, :centerPoint) <= :radius",
            nativeQuery = true)
    Map<String, Object> calculateStatsAroundPoint(
            @Param("centerPoint") Point centerPoint,
            @Param("radius") double radiusInMeters,
            @Param("rentalType") String rentalType);

    // 3. CÁC TRUY VẤN MEDIA VÀ ĐỊA CHỈ
    @Query("SELECT p FROM Property p WHERE p.videoUrl IS NOT NULL AND p.status = 'ACTIVE'")
    Page<Property> findAllWithVideo(Pageable pageable);

    @Query("SELECT p.addressDetail FROM Property p WHERE p.landlordId = :landlordId")
    List<String> findAddressesByLandlordId(@Param("landlordId") Long landlordId);

    // 4. CRONJOB (Tin sắp hết hạn và đã quá hạn)
    @Query("SELECT p FROM Property p WHERE p.status = 'ACTIVE' AND p.expirationDate <= :targetDate AND p.expirationDate > CURRENT_TIMESTAMP")
    List<Property> findExpiringSoon(@Param("targetDate") LocalDateTime targetDate);

    @Query("SELECT p FROM Property p WHERE p.status = 'ACTIVE' AND p.expirationDate <= :now")
    List<Property> findAllExpiredActiveProperties(@Param("now") LocalDateTime now);
}