package com.homeverse.property.repository;

import com.homeverse.property.entity.OwnerReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OwnerReviewRepository extends JpaRepository<OwnerReview, Long> {

    List<OwnerReview> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<OwnerReview> findByOwnerIdAndReviewerIdAndPropertyId(
            Long ownerId,
            Long reviewerId,
            Long propertyId
    );

    long countByOwnerId(Long ownerId);

    @Query("""
            SELECT COALESCE(AVG(r.rating), 0)
            FROM OwnerReview r
            WHERE r.ownerId = :ownerId
            """)
    Double getAverageRating(Long ownerId);
    long countByOwnerIdAndRating(Long ownerId, Integer rating);
    long countByOwnerIdAndVerifiedTrue(Long ownerId);
}