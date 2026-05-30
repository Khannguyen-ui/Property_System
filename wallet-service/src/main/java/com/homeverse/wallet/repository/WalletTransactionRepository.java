package com.homeverse.wallet.repository;

import com.homeverse.wallet.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    
    // Tìm kiếm lịch sử giao dịch theo UserId (kiểu Long) có phân trang
    Page<WalletTransaction> findByUserId(Long userId, Pageable pageable);
}