package com.homeverse.payment.repository;

import com.homeverse.payment.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    List<Bill> findByContractId(Long contractId);
    Optional<Bill> findTopByContractIdOrderByCreatedAtDesc(Long contractId);
}