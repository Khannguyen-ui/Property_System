package com.homeverse.property.repository;

import com.homeverse.property.dto.response.PropertyTypeCountDTO;
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

    Optional<Property> findByIdAndStatus(Long id, Property.Status status);

    Page<Property> findByStatus(Property.Status status, Pageable pageable);

    Page<Property> findByStatusAndExpiresAtAfter(
            Property.Status status,
            LocalDateTime now,
            Pageable pageable
    );

    Optional<Property> findByIdAndStatusAndExpiresAtAfter(
            Long id,
            Property.Status status,
            LocalDateTime now
    );

    List<Property> findByStatusAndExpiresAtBefore(
            Property.Status status,
            LocalDateTime now
    );


    Page<Property> findByOwnerIdAndStatusOrderByCreatedAtDesc(
            Long ownerId,
            Property.Status status,
            Pageable pageable
    );

    Page<Property> findByOwnerIdAndStatusAndPropertyTypeOrderByCreatedAtDesc(
            Long ownerId,
            Property.Status status,
            Property.PropertyType propertyType,
            Pageable pageable
    );

    Page<Property> findByOwnerIdAndStatusAndTransactionTypeOrderByCreatedAtDesc(
            Long ownerId,
            Property.Status status,
            Property.TransactionType transactionType,
            Pageable pageable
    );

    Page<Property> findByOwnerIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            Long ownerId,
            Property.Status status,
            LocalDateTime now,
            Pageable pageable
    );

    Page<Property> findByOwnerIdAndStatusAndTransactionTypeAndExpiresAtAfterOrderByCreatedAtDesc(
            Long ownerId,
            Property.Status status,
            Property.TransactionType transactionType,
            LocalDateTime now,
            Pageable pageable
    );

    @Query(value = "SELECT * FROM properties WHERE id = :id AND status = 'DELETED'", nativeQuery = true)
    Optional<Property> findDeletedById(@Param("id") Long id);

    @Query(
            value = "SELECT * FROM properties WHERE owner_id = :ownerId AND status = 'DELETED'",
            countQuery = "SELECT count(*) FROM properties WHERE owner_id = :ownerId AND status = 'DELETED'",
            nativeQuery = true
    )
    Page<Property> findDeletedByOwnerId(
            @Param("ownerId") Long ownerId,
            Pageable pageable
    );

    @Query(
            value = "SELECT * FROM properties WHERE status = 'DELETED'",
            countQuery = "SELECT count(*) FROM properties WHERE status = 'DELETED'",
            nativeQuery = true
    )
    Page<Property> findAllDeletedProperties(Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE properties SET status = 'PENDING' WHERE id = :id", nativeQuery = true)
    void restoreById(@Param("id") Long id);

    @Modifying
    @Query(value = "UPDATE properties SET status = 'PENDING' WHERE id = ?1 AND status = 'DELETED'", nativeQuery = true)
    int restoreByIdAdmin(Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM properties WHERE id = :id", nativeQuery = true)
    void hardDeleteById(@Param("id") Long id);

    @Modifying
    @Query(value = "DELETE FROM properties WHERE id = ?1 AND status = 'DELETED'", nativeQuery = true)
    int hardDeleteByIdAdmin(Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE properties SET project_id = NULL WHERE project_id = :projectId", nativeQuery = true)
    void detachPropertiesFromProject(@Param("projectId") Long projectId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Property p
            SET p.ownerNameSnapshot = :name,
                p.ownerAvatarSnapshot = :avatar,
                p.ownerSlugSnapshot = :slug,
                p.ownerPhoneSnapshot = :phone
            WHERE p.ownerId = :ownerId
            """)
    void updateOwnerSnapshot(
            @Param("ownerId") Long ownerId,
            @Param("name") String name,
            @Param("avatar") String avatar,
            @Param("slug") String slug,
            @Param("phone") String phone
    );

    @Query("""
            SELECT new com.homeverse.property.dto.response.PropertyTypeCountDTO(
                p.propertyType,
                COUNT(p)
            )
            FROM Property p
            WHERE p.ownerId = :ownerId
              AND p.status = :status
            GROUP BY p.propertyType
            """)
    List<PropertyTypeCountDTO> countByOwnerIdAndStatusGroupByPropertyType(
            @Param("ownerId") Long ownerId,
            @Param("status") Property.Status status
    );

    @Query(value = """
            SELECT * FROM properties p
            WHERE p.status = :status
              AND p.video_url IS NOT NULL
              AND p.video_url <> ''
            ORDER BY p.created_at DESC, p.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Property> findFirstReelsFeed(
            @Param("status") String status,
            @Param("limit") int limit
    );

    @Query(value = """
            SELECT * FROM properties p
            WHERE p.status = :status
              AND p.video_url IS NOT NULL
              AND p.video_url <> ''
              AND (
                    p.created_at < :lastCreatedAt
                    OR (p.created_at = :lastCreatedAt AND p.id < :lastId)
                  )
            ORDER BY p.created_at DESC, p.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Property> findNextReelsFeed(
            @Param("status") String status,
            @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
            @Param("lastId") Long lastId,
            @Param("limit") int limit
    );

    List<Property> findTop10ByIsPromotedTrueOrderByCreatedAtDesc();

    List<Property> findTop10ByOrderByCreatedAtDesc();

    List<Property> findTop10ByVideoUrlIsNotNullOrderByCreatedAtDesc();

    List<Property> findTop10ByIsPromotedTrueAndVideoUrlIsNotNullOrderByCreatedAtDesc();

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
              AND video_url <> ''
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
              AND video_url <> ''
            ORDER BY RANDOM()
            LIMIT 100
            """, nativeQuery = true)
    List<Property> findRandomReels();

    long countByOwnerIdAndStatus(Long ownerId, Property.Status status);

    long countByOwnerIdAndIsPromotedTrueAndStatus(
            Long ownerId,
            Property.Status status
    );

    Page<Property> findByOwnerIdOrderByCreatedAtDesc(
            Long ownerId,
            Pageable pageable
    );

    Page<Property> findByOwnerIdAndTransactionTypeOrderByCreatedAtDesc(
            Long ownerId,
            Property.TransactionType transactionType,
            Pageable pageable
    );

    @Query("""
                SELECT p
                FROM Property p
                WHERE p.projectId = :projectId
                  AND p.status = :status
                  AND p.expiresAt > :now
                ORDER BY p.createdAt DESC
            """)
    Page<Property> findPublicByProjectId(
            @Param("projectId") Long projectId,
            @Param("status") Property.Status status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );


}