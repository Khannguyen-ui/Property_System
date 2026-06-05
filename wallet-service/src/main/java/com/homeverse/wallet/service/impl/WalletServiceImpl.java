package com.homeverse.wallet.service.impl;

import com.homeverse.wallet.entity.Wallet;
import com.homeverse.wallet.entity.WalletTransaction;
import com.homeverse.wallet.repository.WalletRepository;
import com.homeverse.wallet.repository.WalletTransactionRepository;
import com.homeverse.wallet.service.WalletService;
import com.homeverse.wallet.dto.NotificationEvent;
import com.homeverse.wallet.kafka.WalletNotificationProducer;
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
    private final WalletNotificationProducer notificationProducer;


    @Override
    @Transactional
    public void handleTransaction(Long userId, BigDecimal amount, String type, String referenceId, String description) {
        Wallet wallet = getOrCreateWallet(userId);

        BigDecimal availableBalance = wallet.getBalance().subtract(wallet.getHoldBalance());

        if (amount.compareTo(BigDecimal.ZERO) < 0
                && availableBalance.add(amount).compareTo(BigDecimal.ZERO) < 0) {
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
        Wallet wallet = getOrCreateWallet(userId);

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
        Wallet wallet = getOrCreateWallet(userId);

        if (wallet.getHoldBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Số tiền đang giữ không đủ để hoàn cọc!");
        }

        wallet.setHoldBalance(wallet.getHoldBalance().subtract(amount));
        walletRepository.save(wallet);

        saveTransaction(wallet, userId, amount, "RELEASE", referenceId, "Hoàn tiền đặt cọc");
    }

    @Override
    @Transactional
    public void captureMoney(Long userId, BigDecimal amount, String referenceId) {
        Wallet wallet = getOrCreateWallet(userId);

        if (wallet.getHoldBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Số tiền đang giữ không đủ để xác nhận thanh toán!");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setHoldBalance(wallet.getHoldBalance().subtract(amount));
        walletRepository.save(wallet);

        saveTransaction(wallet, userId, amount.negate(), "PAYMENT_CONFIRMED", referenceId, "Xác nhận thanh toán từ tiền cọc");
    }

    @Override
    @Transactional
    public void debit(Long userId, BigDecimal amount) {
        Wallet wallet = getOrCreateWallet(userId);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Số dư ví không đủ để thực hiện giao dịch!");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        saveTransaction(wallet, userId, amount.negate(), "DEBIT", "PAYMENT_SERVICE", "Trừ tiền mua gói dịch vụ");
        log.info("Đã trừ {} từ ví user {}. Số dư còn lại: {}", amount, userId, wallet.getBalance());
    }

    @Override
    @Transactional
    public Wallet getWalletByUserId(Long userId) {
        return getOrCreateWallet(userId);
    }

    @Override
    @Transactional
    public Page<WalletTransaction> getTransactions(Long userId, int page, int size) {
        getOrCreateWallet(userId);
        return transactionRepository.findByUserId(userId, PageRequest.of(page, size));
    }

    private Wallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId)
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

        notificationProducer.send(NotificationEvent.builder()
                .receiverId(userId)
                .title(buildNotificationTitle(type))
                .content(buildNotificationContent(type, amount, wallet.getBalance(), description))
                .type("WALLET_" + type)
                .referenceId(referenceId)
                .build());
    }
    private String buildNotificationTitle(String type) {
        return switch (type) {
            case "DEPOSIT" -> "Nạp tiền thành công";
            case "DEBIT" -> "Trừ tiền ví thành công";
            case "HOLD" -> "Đã giữ tiền trong ví";
            case "RELEASE" -> "Đã hoàn tiền cọc";
            case "PAYMENT_CONFIRMED" -> "Thanh toán đã xác nhận";
            case "REFUND" -> "Hoàn tiền thành công";
            default -> "Cập nhật giao dịch ví";
        };
    }

    private String buildNotificationContent(String type, BigDecimal amount, BigDecimal balance, String description) {
        BigDecimal displayAmount = amount.abs();

        String action = switch (type) {
            case "DEPOSIT" -> "Bạn vừa nạp";
            case "DEBIT" -> "Ví của bạn vừa bị trừ";
            case "HOLD" -> "Ví của bạn vừa bị giữ";
            case "RELEASE" -> "Bạn vừa được hoàn";
            case "PAYMENT_CONFIRMED" -> "Bạn vừa thanh toán";
            case "REFUND" -> "Bạn vừa được hoàn tiền";
            default -> "Ví của bạn vừa phát sinh giao dịch";
        };

        return action + " " + displayAmount.toPlainString()
                + " VND. Số dư hiện tại: " + balance.toPlainString()
                + " VND. " + (description == null ? "" : description);
    }
}