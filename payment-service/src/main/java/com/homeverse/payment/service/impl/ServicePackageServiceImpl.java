package com.homeverse.payment.service.impl;

import com.homeverse.common.dto.PaymentEvent;
import com.homeverse.payment.entity.ServicePackage;
import com.homeverse.payment.entity.Transaction;
import com.homeverse.payment.repository.ServicePackageRepository;
import com.homeverse.payment.repository.TransactionRepository;
import com.homeverse.payment.service.ServicePackageService;
import com.homeverse.payment.kafka.PaymentProducer;
import com.homeverse.payment.client.WalletClient;
import com.homeverse.payment.entity.PackageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServicePackageServiceImpl implements ServicePackageService {

    private final TransactionRepository transactionRepository;
    private final ServicePackageRepository packageRepository;
    private final PaymentProducer paymentProducer;
    private final WalletClient walletClient;

    @Override
    @Transactional
    public void buyMembership(Long userId, Long packageId) {
        try {
        log.info("Đang xử lý Promotion cho User {} - Package {}", userId, packageId);
        ServicePackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new RuntimeException("Gói dịch vụ không tồn tại"));

        walletClient.debit(userId, pkg.getPrice());

        Transaction trans = Transaction.builder()
                .userId(userId)
                .amount(pkg.getPrice())
                .type(pkg.getType().toString())
                .description("Mua gói: " + pkg.getName())
                .status("SUCCESS")
                .createdAt(LocalDateTime.now())
                .build();
        
        Transaction savedTrans = transactionRepository.save(trans);

        PaymentEvent event = PaymentEvent.builder()
                .userId(userId)
                .amount(pkg.getPrice())
                .packageId(packageId)
                .packageName(pkg.getName())
                .transactionId(savedTrans.getId().toString())
                .type(pkg.getType().toString())
                .durationDays(pkg.getDurationDays())
                .priorityLevel(pkg.getPriorityLevel() != null ? pkg.getPriorityLevel() : 0)
                .build();

        paymentProducer.sendPaymentSuccess(event);
        } catch (Exception e) {
        log.error("LỖI CHI TIẾT TẠI ĐÂY NÈ ÂN: ", e); // Dòng này sẽ in ra chính xác lỗi gì
        throw e;
    }
    }

    @Override
    @Transactional
    public void buyPromotion(Long userId, Long packageId, Long propertyId) {
        ServicePackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy gói cước!"));

        walletClient.debit(userId, pkg.getPrice());

        Transaction transaction = Transaction.builder()
                .userId(userId)
                .amount(pkg.getPrice())
                .type(PackageType.ROOM_PROMOTION.toString())
                .status("SUCCESS")
                .description("Mua gói đẩy tin cho bài đăng ID: " + propertyId)
                .createdAt(LocalDateTime.now())
                .build();
        
        Transaction savedTrans = transactionRepository.save(transaction);

        PaymentEvent event = PaymentEvent.builder()
                .userId(userId)
                .roomId(propertyId)
                .amount(pkg.getPrice())
                .packageId(packageId)
                .packageName(pkg.getName())
                .type(PackageType.ROOM_PROMOTION.toString())
                .durationDays(pkg.getDurationDays())
                .transactionId(savedTrans.getId().toString())
                .build();

        paymentProducer.sendPaymentSuccess(event);
    }
}