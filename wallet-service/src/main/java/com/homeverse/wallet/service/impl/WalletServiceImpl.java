package com.homeverse.wallet.service.impl;

import com.homeverse.wallet.entity.Wallet;
import com.homeverse.wallet.entity.WalletTransaction;
import com.homeverse.wallet.repository.WalletRepository;
import com.homeverse.wallet.repository.WalletTransactionRepository;
import com.homeverse.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    @Override
    @Transactional
    public void handleTransaction(Long userId, BigDecimal amount, String type, String referenceId, String description) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Tạo ví mới cho User: {}", userId);
                    return walletRepository.save(Wallet.builder()
                            .userId(userId)
                            .balance(BigDecimal.ZERO)
                            .holdBalance(BigDecimal.ZERO)
                            .status("ACTIVE")
                            .currency("VND")
                            .build());
                });

        // Kiểm tra số dư khả dụng: (balance - holdBalance) + amount < 0 (khi amount âm)
        BigDecimal availableBalance = wallet.getBalance().subtract(wallet.getHoldBalance());
        if (amount.compareTo(BigDecimal.ZERO) < 0 && availableBalance.add(amount).compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Số dư không đủ để thực hiện giao dịch!");
        }

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        saveTransaction(wallet, userId, amount, type, referenceId, description);
        log.info("Giao dịch {} thành công cho User {}: {} VND", type, userId, amount);
    }

    @Override
    @Transactional
    public void holdMoney(Long userId, BigDecimal amount, String referenceId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví!"));

        BigDecimal availableBalance = wallet.getBalance().subtract(wallet.getHoldBalance());
        if (availableBalance.compareTo(amount) < 0) {
            throw new RuntimeException("Số dư khả dụng không đủ để đặt cọc!");
        }

        wallet.setHoldBalance(wallet.getHoldBalance().add(amount));
        walletRepository.save(wallet);

        saveTransaction(wallet, userId, amount.negate(), "HOLD", referenceId, "Giữ tiền đặt cọc");
    }

    @Override
    @Transactional
    public void releaseMoney(Long userId, BigDecimal amount, String referenceId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví!"));

        wallet.setHoldBalance(wallet.getHoldBalance().subtract(amount));
        walletRepository.save(wallet);

        saveTransaction(wallet, userId, amount, "RELEASE", referenceId, "Hoàn tiền đặt cọc");
    }

    @Override
    @Transactional
    public void captureMoney(Long userId, BigDecimal amount, String referenceId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví!"));

        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setHoldBalance(wallet.getHoldBalance().subtract(amount));
        walletRepository.save(wallet);

        saveTransaction(wallet, userId, amount.negate(), "PAYMENT_CONFIRMED", referenceId, "Xác nhận thanh toán từ tiền cọc");
    }

    @Override
    @Transactional
    public void debit(Long userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví của user: " + userId));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Số dư ví không đủ để thực hiện giao dịch!");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
        
        saveTransaction(wallet, userId, amount.negate(), "DEBIT", "PAYMENT_SERVICE", "Trừ tiền mua gói dịch vụ");
        log.info("Đã trừ {} từ ví user {}. Số dư còn lại: {}", amount, userId, wallet.getBalance());
    }

    @Override
    public Wallet getWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
    }

    @Override
    public Page<WalletTransaction> getTransactions(Long userId, int page, int size) {
        return transactionRepository.findByUserId(userId, PageRequest.of(page, size));
    }

    private void saveTransaction(Wallet wallet, Long userId, BigDecimal amount, String type, String referenceId, String description) {
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .userId(userId)
                .amount(amount)
                .transactionType(type)
                .referenceId(referenceId)
                .description(description)
                .status("COMPLETED")
                .build();
        transactionRepository.save(transaction);
    }
}