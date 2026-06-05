package com.homeverse.property.repository;

import com.homeverse.property.entity.OwnerFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OwnerFollowRepository extends JpaRepository<OwnerFollow, Long> {

    Optional<OwnerFollow> findByFollowerIdAndOwnerId(Long followerId, Long ownerId);

    boolean existsByFollowerIdAndOwnerId(Long followerId, Long ownerId);

    long countByOwnerId(Long ownerId);

    long countByFollowerId(Long followerId);

    @Query("SELECT f.ownerId FROM OwnerFollow f WHERE f.followerId = :followerId")
    List<Long> findOwnerIdsByFollowerId(Long followerId);
}