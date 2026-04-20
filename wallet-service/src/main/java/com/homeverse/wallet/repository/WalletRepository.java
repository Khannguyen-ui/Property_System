package com.homeverse.wallet.repository;

import com.homeverse.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    // Tìm ví theo ID người dùng
   // WalletRepository.java
Optional<Wallet> findByUserId(Long userId);
}