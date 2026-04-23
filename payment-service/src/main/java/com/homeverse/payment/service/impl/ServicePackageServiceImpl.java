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
import java.util.List;

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
            log.info("Đang xử lý Membership cho User {} - Package {}", userId, packageId);
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
                    .quotaLimit(pkg.getQuotaLimit() != null ? pkg.getQuotaLimit() : 0)
                    .build();

            paymentProducer.sendPaymentSuccess(event);
        } catch (Exception e) {
            log.error("LỖI CHI TIẾT TẠI ĐÂY NÈ : ", e);
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
                .priorityLevel(pkg.getPriorityLevel() != null ? pkg.getPriorityLevel() : 0)
                .build();

        paymentProducer.sendPaymentSuccess(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServicePackage> getAllPackagesForAdmin() {
        return packageRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServicePackage> getAllActivePackages() {
        return packageRepository.findAll()
                .stream()
                .filter(ServicePackage::getActive)
                .toList();
    }

    @Override
    @Transactional
    public ServicePackage createPackage(ServicePackage pkg) {
        log.info("Admin đang tạo gói cước mới: {}", pkg.getName());
        if (pkg.getActive() == null) pkg.setActive(true);
        return packageRepository.save(pkg);
    }

    @Override
    @Transactional
    public ServicePackage updatePackage(Long id, ServicePackage pkg) {
        ServicePackage existingPkg = packageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy gói cước ID: " + id));

        log.info("Admin đang cập nhật gói cước ID: {}", id);
        
        existingPkg.setName(pkg.getName());
        existingPkg.setPrice(pkg.getPrice());
        existingPkg.setDurationDays(pkg.getDurationDays());
        existingPkg.setDiscountPercent(pkg.getDiscountPercent());
        existingPkg.setPriorityLevel(pkg.getPriorityLevel());
        existingPkg.setQuotaLimit(pkg.getQuotaLimit());
        existingPkg.setType(pkg.getType());
        existingPkg.setDescription(pkg.getDescription());
        existingPkg.setActive(pkg.getActive());

        return packageRepository.save(existingPkg);
    }

    @Override
    @Transactional
    public void deletePackage(Long id) {
        ServicePackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy gói cước ID: " + id));

        log.warn("Admin thực hiện ẩn gói cước ID: {}", id);
        pkg.setActive(false);
        packageRepository.save(pkg);
    }
}