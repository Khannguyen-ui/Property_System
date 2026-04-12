package com.homeverse.property.repository;

import com.homeverse.property.entity.OwnerQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OwnerQuotaRepository extends JpaRepository<OwnerQuota, Long> {

}