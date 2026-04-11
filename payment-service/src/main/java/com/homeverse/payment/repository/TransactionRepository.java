package com.homeverse.payment.repository;

import com.homeverse.payment.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Tìm lịch sử giao dịch của 1 user (sắp xếp mới nhất lên đầu)
    List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndStatus(Long userId, String status);


    boolean existsByVnpayCode(String vnpayCode);
}