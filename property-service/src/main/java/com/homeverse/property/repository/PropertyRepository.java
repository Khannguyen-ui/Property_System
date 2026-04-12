package com.homeverse.property.repository;

import com.homeverse.property.entity.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {


    @Query(value = "SELECT * FROM properties WHERE id = :id AND status = 'DELETED'", nativeQuery = true)
    Optional<Property> findDeletedById(@Param("id") Long id);



    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM properties WHERE id = :id", nativeQuery = true)
    void hardDeleteById(@Param("id") Long id);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE properties SET status = 'PENDING' WHERE id = :id", nativeQuery = true)
    void restoreById(@Param("id") Long id);



    @Query(
            value = "SELECT * FROM properties WHERE owner_id = :ownerId AND status = 'DELETED'",
            countQuery = "SELECT count(*) FROM properties WHERE owner_id = :ownerId AND status = 'DELETED'",
            nativeQuery = true
    )
    org.springframework.data.domain.Page<Property> findDeletedByOwnerId(
            @Param("ownerId") Long ownerId,
            org.springframework.data.domain.Pageable pageable
    );
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE properties SET project_id = NULL WHERE project_id = :projectId", nativeQuery = true)
    void detachPropertiesFromProject(@Param("projectId") Long projectId);
    Page<Property> findByStatus(Property.Status status, Pageable pageable);
    Optional<Property> findByIdAndStatus(Long id, Property.Status status);
    // 1. Lấy toàn bộ thùng rác (Bất chấp của ai)
    @Query(value = "SELECT * FROM properties WHERE status = 'DELETED'",
            countQuery = "SELECT count(*) FROM properties WHERE status = 'DELETED'",
            nativeQuery = true)
    Page<Property> findAllDeletedProperties(Pageable pageable);

    // 2. Khôi phục bài đăng (Admin - Khôi phục về trạng thái PENDING để xem xét lại)
    @Modifying
    @Query(value = "UPDATE properties SET status = 'PENDING' WHERE id = ?1 AND status = 'DELETED'", nativeQuery = true)
    int restoreByIdAdmin(Long id);

    // 3. Tiễn vĩnh viễn (Admin)
    @Modifying
    @Query(value = "DELETE FROM properties WHERE id = ?1 AND status = 'DELETED'", nativeQuery = true)
    int hardDeleteByIdAdmin(Long id);

    // Bài đăng thuộc về
    // Cập nhật đồng loạt Snapshot của chủ nhà cho tất cả bài đăng
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Property p SET p.ownerNameSnapshot = :name, p.ownerAvatarSnapshot = :avatar, p.ownerSlugSnapshot = :slug WHERE p.ownerId = :ownerId")
    void updateOwnerSnapshot(
            @Param("ownerId") Long ownerId,
            @Param("name") String name,
            @Param("avatar") String avatar,
            @Param("slug") String slug
    );

    // 1. Dành cho màn hình lướt Video (Chỉ lấy bài ACTIVE và CÓ VIDEO)

    @Query(value = """
    SELECT * FROM property p 
    WHERE p.status = :status 
      AND p.video_url IS NOT NULL 
      AND p.video_url != ''
      AND (:lastCreatedAt IS NULL 
           OR p.created_at < :lastCreatedAt 
           OR (p.created_at = :lastCreatedAt AND p.id < :lastId))
    ORDER BY p.created_at DESC, p.id DESC
    FETCH FIRST :limit ROWS ONLY
    """, nativeQuery = true)
    List<Property> findReelsFeed(
            @Param("status") Property.Status status,
            @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
            @Param("lastId") Long lastId,
            @Param("limit") int limit);
    // 2. Dành cho màn hình Xem trang cá nhân (Lấy tất cả bài của 1 User)
    org.springframework.data.domain.Page<Property> findByOwnerIdAndStatusOrderByCreatedAtDesc(Long ownerId, Property.Status status, org.springframework.data.domain.Pageable pageable);


}