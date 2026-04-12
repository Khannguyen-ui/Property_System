package com.homeverse.property.repository;

import com.homeverse.property.entity.UserPropertyInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InteractionRepository extends JpaRepository<UserPropertyInteraction, Long> {

    /**
     * 1. Tìm 1 tương tác cụ thể (Dùng cho logic Toggle Like/Save)
     * Thay thế cho: findByUserIdAndPropertyIdAndInteractionType
     */
    @Query("SELECT u FROM UserPropertyInteraction u WHERE " +
            "(u.userId = :userId OR (u.userId IS NULL AND u.guestId = :guestId)) " +
            "AND u.propertyId = :propertyId AND u.interactionType = :type")
    Optional<UserPropertyInteraction> findInteraction(
            @Param("userId") Long userId,
            @Param("guestId") String guestId,
            @Param("propertyId") Long propertyId,
            @Param("type") UserPropertyInteraction.InteractionType type);

    /**
     * 2. Kiểm tra xem tương tác có tồn tại không
     * Thay thế cho: existsByUserIdAndPropertyIdAndInteractionType
     */
    @Query("SELECT COUNT(u) > 0 FROM UserPropertyInteraction u WHERE " +
            "(u.userId = :userId OR (u.userId IS NULL AND u.guestId = :guestId)) " +
            "AND u.propertyId = :propertyId AND u.interactionType = :type")
    boolean existsInteraction(
            @Param("userId") Long userId,
            @Param("guestId") String guestId,
            @Param("propertyId") Long propertyId,
            @Param("type") UserPropertyInteraction.InteractionType type);


    @Query("SELECT u FROM UserPropertyInteraction u WHERE " +
            "(u.userId = :userId OR (u.userId IS NULL AND u.guestId = :guestId)) " +
            "AND u.propertyId IN :propertyIds")
    List<UserPropertyInteraction> findInteractionsIn(
            @Param("userId") Long userId,
            @Param("guestId") String guestId,
            @Param("propertyIds") List<Long> propertyIds);
}