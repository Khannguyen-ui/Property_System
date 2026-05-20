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

        @Query(value = "SELECT * FROM properties WHERE owner_id = :ownerId AND status = 'DELETED'", countQuery = "SELECT count(*) FROM properties WHERE owner_id = :ownerId AND status = 'DELETED'", nativeQuery = true)
        Page<Property> findDeletedByOwnerId(
                        @Param("ownerId") Long ownerId,
                        Pageable pageable);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query(value = "UPDATE properties SET project_id = NULL WHERE project_id = :projectId", nativeQuery = true)
        void detachPropertiesFromProject(@Param("projectId") Long projectId);

        Page<Property> findByStatus(Property.Status status, Pageable pageable);

        Optional<Property> findByIdAndStatus(Long id, Property.Status status);

        @Query(value = "SELECT * FROM properties WHERE status = 'DELETED'", countQuery = "SELECT count(*) FROM properties WHERE status = 'DELETED'", nativeQuery = true)
        Page<Property> findAllDeletedProperties(Pageable pageable);

        @Modifying
        @Query(value = "UPDATE properties SET status = 'PENDING' WHERE id = ?1 AND status = 'DELETED'", nativeQuery = true)
        int restoreByIdAdmin(Long id);

        @Modifying
        @Query(value = "DELETE FROM properties WHERE id = ?1 AND status = 'DELETED'", nativeQuery = true)
        int hardDeleteByIdAdmin(Long id);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("""
                            UPDATE Property p
                            SET p.ownerNameSnapshot = :name,
                                p.ownerAvatarSnapshot = :avatar,
                                p.ownerSlugSnapshot = :slug
                            WHERE p.ownerId = :ownerId
                        """)
        void updateOwnerSnapshot(
                        @Param("ownerId") Long ownerId,
                        @Param("name") String name,
                        @Param("avatar") String avatar,
                        @Param("slug") String slug);

        @Query(value = """
                            SELECT *
                            FROM property p
                            WHERE p.status = :status
                              AND p.video_url IS NOT NULL
                              AND p.video_url != ''
                              AND (
                                    :lastCreatedAt IS NULL
                                    OR p.created_at < :lastCreatedAt
                                    OR (
                                        p.created_at = :lastCreatedAt
                                        AND p.id < :lastId
                                    )
                              )
                            ORDER BY p.created_at DESC, p.id DESC
                            FETCH FIRST :limit ROWS ONLY
                        """, nativeQuery = true)
        List<Property> findReelsFeed(
                        @Param("status") Property.Status status,
                        @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
                        @Param("lastId") Long lastId,
                        @Param("limit") int limit);

        Page<Property> findByOwnerIdAndStatusOrderByCreatedAtDesc(
                        Long ownerId,
                        Property.Status status,
                        Pageable pageable);

        List<Property> findTop10ByIsPromotedTrueOrderByCreatedAtDesc();

        List<Property> findTop10ByOrderByCreatedAtDesc();

        List<Property> findTop10ByIsPromotedTrueAndVideoUrlIsNotNullOrderByCreatedAtDesc();

        List<Property> findTop10ByVideoUrlIsNotNullOrderByCreatedAtDesc();

        @Query(value = """
                            SELECT *
                            FROM properties
                            WHERE is_promoted = true
                              AND status = 'ACTIVE'
                            ORDER BY created_at DESC
                            LIMIT 100
                        """, nativeQuery = true)
        List<Property> findPromotedProperties();

        @Query(value = """
                            SELECT *
                            FROM properties
                            WHERE is_promoted = true
                              AND status = 'ACTIVE'
                              AND video_url IS NOT NULL
                              AND video_url != ''
                            ORDER BY created_at DESC
                            LIMIT 100
                        """, nativeQuery = true)
        List<Property> findPromotedReels();

        @Query(value = """
                            SELECT *
                            FROM properties
                            WHERE status = 'ACTIVE'
                            ORDER BY RANDOM()
                            LIMIT 100
                        """, nativeQuery = true)
        List<Property> findRandomProperties();

        @Query(value = """
                            SELECT *
                            FROM properties
                            WHERE status = 'ACTIVE'
                              AND video_url IS NOT NULL
                              AND video_url != ''
                            ORDER BY RANDOM()
                            LIMIT 100
                        """, nativeQuery = true)
        List<Property> findRandomReels();

        long countByOwnerIdAndStatus(Long ownerId, Property.Status status);

        long countByOwnerIdAndIsPromotedTrueAndStatus(
                        Long ownerId,
                        Property.Status status);
}