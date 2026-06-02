package com.homeverse.property.repository;

import com.homeverse.property.entity.PropertyComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PropertyCommentRepository extends JpaRepository<PropertyComment, Long> {

    Page<PropertyComment> findByPropertyIdAndParentIdIsNullAndStatusOrderByCreatedAtDesc(
            Long propertyId,
            PropertyComment.Status status,
            Pageable pageable
    );

    Page<PropertyComment> findByParentIdAndStatusOrderByCreatedAtAsc(
            Long parentId,
            PropertyComment.Status status,
            Pageable pageable
    );

    long countByPropertyIdAndStatus(Long propertyId, PropertyComment.Status status);

    long countByParentIdAndStatus(Long parentId, PropertyComment.Status status);

    @Modifying
    @Query("""
        UPDATE PropertyComment c
        SET c.status = 'DELETED'
        WHERE c.parentId = :parentId
    """)
    int softDeleteRepliesByParentId(Long parentId);
    long countByPropertyId(Long propertyId);
}