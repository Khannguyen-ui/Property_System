package com.homeverse.customer.repository;

import com.homeverse.customer.entity.Customer;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByPublicId(String publicId);
    boolean existsByCitizenIdAndIdNot(String citizenId, Long currentCustomerId);
    @Query("SELECT c.citizenId FROM Customer c WHERE c.citizenId IS NOT NULL")
    Slice<String> findCitizenIdsInBatches(Pageable pageable);
    long countByCitizenIdIsNotNull();
}