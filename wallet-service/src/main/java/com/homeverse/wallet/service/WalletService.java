package com.homeverse.wallet.service;

import com.homeverse.wallet.entity.Wallet;
import com.homeverse.wallet.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import java.math.BigDecimal;

public interface WalletService {
    // Đổi hết Long amount sang BigDecimal amount
    void handleTransaction(Long userId, BigDecimal amount, String type, String referenceId, String description);
    
    void holdMoney(Long userId, BigDecimal amount, String referenceId);
    
    void releaseMoney(Long userId, BigDecimal amount, String referenceId);
    
    void captureMoney(Long userId, BigDecimal amount, String referenceId);
    
    void debit(Long userId, BigDecimal amount);

    Wallet getWalletByUserId(Long userId);
    
    Page<WalletTransaction> getTransactions(Long userId, int page, int size);
}