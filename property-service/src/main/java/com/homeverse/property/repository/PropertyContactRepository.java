package com.homeverse.property.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.homeverse.property.entity.PropertyContact;

public interface PropertyContactRepository extends JpaRepository<PropertyContact, Long> {

    boolean existsByUserIdAndOwnerId(Long userId, Long ownerId);

    boolean existsByUserIdAndPropertyId(Long userId, Long propertyId);
}
