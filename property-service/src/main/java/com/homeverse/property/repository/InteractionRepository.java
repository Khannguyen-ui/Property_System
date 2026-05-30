package com.homeverse.property.repository;

import com.homeverse.property.entity.Property;
import com.homeverse.property.entity.UserPropertyInteraction;
import com.homeverse.property.entity.UserPropertyInteraction.InteractionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InteractionRepository extends JpaRepository<UserPropertyInteraction, Long> {

    @Query("""
            SELECT u FROM UserPropertyInteraction u
            WHERE u.propertyId = :propertyId
              AND u.interactionType = :type
              AND (
                    (:userId IS NOT NULL AND u.userId = :userId)
                    OR
                    (:userId IS NULL AND :guestId IS NOT NULL AND u.guestId = :guestId)
                  )
            """)
    Optional<UserPropertyInteraction> findInteraction(
            @Param("userId") Long userId,
            @Param("guestId") String guestId,
            @Param("propertyId") Long propertyId,
            @Param("type") InteractionType type
    );

    @Query("""
            SELECT COUNT(u) > 0 FROM UserPropertyInteraction u
            WHERE u.propertyId = :propertyId
              AND u.interactionType = :type
              AND (
                    (:userId IS NOT NULL AND u.userId = :userId)
                    OR
                    (:userId IS NULL AND :guestId IS NOT NULL AND u.guestId = :guestId)
                  )
            """)
    boolean existsInteraction(
            @Param("userId") Long userId,
            @Param("guestId") String guestId,
            @Param("propertyId") Long propertyId,
            @Param("type") InteractionType type
    );

    @Query("""
            SELECT u FROM UserPropertyInteraction u
            WHERE u.propertyId IN :propertyIds
              AND (
                    (:userId IS NOT NULL AND u.userId = :userId)
                    OR
                    (:userId IS NULL AND :guestId IS NOT NULL AND u.guestId = :guestId)
                  )
            """)
    List<UserPropertyInteraction> findInteractionsIn(
            @Param("userId") Long userId,
            @Param("guestId") String guestId,
            @Param("propertyIds") List<Long> propertyIds
    );

    @Query("""
            SELECT p FROM Property p
            JOIN UserPropertyInteraction u ON u.propertyId = p.id
            WHERE u.interactionType = :type
              AND p.status = :status
              AND (
                    (:userId IS NOT NULL AND u.userId = :userId)
                    OR
                    (:userId IS NULL AND :guestId IS NOT NULL AND u.guestId = :guestId)
                  )
            ORDER BY u.id DESC
            """)
    Page<Property> findPropertiesByInteraction(
            @Param("userId") Long userId,
            @Param("guestId") String guestId,
            @Param("type") InteractionType type,
            @Param("status") Property.Status status,
            Pageable pageable
    );
}