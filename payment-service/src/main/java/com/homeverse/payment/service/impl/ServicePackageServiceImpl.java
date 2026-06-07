package com.homeverse.payment.service.impl;

import com.homeverse.common.dto.PaymentEvent;
import com.homeverse.payment.client.PropertyClient;
import com.homeverse.payment.client.WalletClient;
import com.homeverse.payment.dto.PropertyResponseDTO;
import com.homeverse.payment.entity.PackageType;
import com.homeverse.payment.entity.ServicePackage;
import com.homeverse.payment.entity.Transaction;
import com.homeverse.payment.kafka.PaymentProducer;
import com.homeverse.payment.repository.ServicePackageRepository;
import com.homeverse.payment.repository.TransactionRepository;
import com.homeverse.payment.service.ServicePackageService;
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
    private final PropertyClient propertyClient;

    @Override
    @Transactional
    public void buyMembership(Long userId, Long packageId) {
        try {
            log.info("Đang xử lý Membership cho User {} - Package {}", userId, packageId);

            ServicePackage pkg = packageRepository.findById(packageId)
                    .orElseThrow(() -> new RuntimeException("Gói dịch vụ không tồn tại"));

            validateMembershipPackage(pkg);

            walletClient.debit(userId, pkg.getPrice());

            Transaction trans = Transaction.builder()
                    .userId(userId)
                    .amount(pkg.getPrice())
                    .type(PackageType.MEMBERSHIP.toString())
                    .description("Mua gói đăng bài/hội viên: " + pkg.getName())
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
                    .type(PackageType.MEMBERSHIP.toString())
                    .durationDays(pkg.getDurationDays() != null ? pkg.getDurationDays() : 0)
                    .priorityLevel(0)
                    .quotaLimit(pkg.getQuotaLimit())
                    .build();

            paymentProducer.sendPaymentSuccess(event);

        } catch (Exception e) {
            log.error("Lỗi khi mua gói đăng bài/hội viên. userId={}, packageId={}", userId, packageId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void buyPromotion(Long userId, Long packageId, Long propertyId) {
        try {
            log.info("Đang xử lý đẩy tin cho User {} - Package {} - Property {}", userId, packageId, propertyId);

            ServicePackage pkg = packageRepository.findById(packageId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy gói cước"));

            validatePromotionPackage(pkg);

            if (propertyId == null) {
                throw new RuntimeException("Thiếu ID bài đăng cần đẩy tin");
            }
            PropertyResponseDTO property = propertyClient.getProperty(propertyId);

            if (property == null) {
                throw new RuntimeException("Không tìm thấy bài đăng cần đẩy tin");
            }

            if (!userId.equals(property.getOwnerId())) {
                throw new RuntimeException("Bạn không có quyền mua gói đẩy tin cho bài đăng này");
            }

            if (!"ACTIVE".equalsIgnoreCase(property.getStatus())) {
                throw new RuntimeException("Chỉ có thể mua gói đẩy tin cho bài đăng đã được duyệt và đang hiển thị");
            }

            if (property.getExpiresAt() != null && property.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Bài đăng đã hết hạn, không thể mua gói đẩy tin");
            }

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
                    .priorityLevel(pkg.getPriorityLevel())
                    .quotaLimit(0)
                    .build();

            paymentProducer.sendPaymentSuccess(event);

        } catch (Exception e) {
            log.error(
                    "Lỗi khi mua gói đẩy tin. userId={}, packageId={}, propertyId={}",
                    userId,
                    packageId,
                    propertyId,
                    e
            );
            throw e;
        }
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
                .filter(pkg -> Boolean.TRUE.equals(pkg.getActive()))
                .toList();
    }

    @Override
    @Transactional
    public ServicePackage createPackage(ServicePackage pkg) {
        log.info("Admin đang tạo gói cước mới: {}", pkg.getName());

        normalizePackage(pkg);
        validatePackageForSave(pkg);

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

        normalizePackage(existingPkg);
        validatePackageForSave(existingPkg);

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

    private void validateMembershipPackage(ServicePackage pkg) {
        validateBasicPackage(pkg);

        if (pkg.getType() != PackageType.MEMBERSHIP) {
            throw new RuntimeException("Gói này không phải gói đăng bài/hội viên");
        }

        if (pkg.getQuotaLimit() == null || pkg.getQuotaLimit() <= 0) {
            throw new RuntimeException("Gói đăng bài chưa cấu hình số lượt đăng");
        }
    }

    private void validatePromotionPackage(ServicePackage pkg) {
        validateBasicPackage(pkg);

        if (pkg.getType() != PackageType.ROOM_PROMOTION) {
            throw new RuntimeException("Gói này không phải gói đẩy tin");
        }

        if (pkg.getDurationDays() == null || pkg.getDurationDays() <= 0) {
            throw new RuntimeException("Gói đẩy tin chưa cấu hình thời hạn");
        }

        if (pkg.getPriorityLevel() == null || pkg.getPriorityLevel() <= 0) {
            throw new RuntimeException("Gói đẩy tin chưa cấu hình cấp độ ưu tiên");
        }
    }

    private void validateBasicPackage(ServicePackage pkg) {
        if (pkg.getType() == null) {
            throw new RuntimeException("Gói dịch vụ chưa cấu hình loại gói");
        }

        if (!Boolean.TRUE.equals(pkg.getActive())) {
            throw new RuntimeException("Gói dịch vụ đã ngừng hoạt động");
        }

        if (pkg.getPrice() == null || pkg.getPrice().signum() <= 0) {
            throw new RuntimeException("Gói dịch vụ chưa cấu hình giá hợp lệ");
        }
    }

    private void validatePackageForSave(ServicePackage pkg) {
        if (pkg.getName() == null || pkg.getName().isBlank()) {
            throw new RuntimeException("Tên gói không được để trống");
        }

        if (pkg.getType() == null) {
            throw new RuntimeException("Loại gói không được để trống");
        }

        if (pkg.getPrice() == null || pkg.getPrice().signum() <= 0) {
            throw new RuntimeException("Giá gói phải lớn hơn 0");
        }

        if (pkg.getType() == PackageType.MEMBERSHIP) {
            if (pkg.getQuotaLimit() == null || pkg.getQuotaLimit() <= 0) {
                throw new RuntimeException("Gói đăng bài phải có số lượt đăng lớn hơn 0");
            }
        }

        if (pkg.getType() == PackageType.ROOM_PROMOTION) {
            if (pkg.getDurationDays() == null || pkg.getDurationDays() <= 0) {
                throw new RuntimeException("Gói đẩy tin phải có thời hạn lớn hơn 0 ngày");
            }

            if (pkg.getPriorityLevel() == null || pkg.getPriorityLevel() <= 0) {
                throw new RuntimeException("Gói đẩy tin phải có cấp ưu tiên lớn hơn 0");
            }
        }
    }

    private void normalizePackage(ServicePackage pkg) {
        if (pkg.getActive() == null) {
            pkg.setActive(true);
        }

        if (pkg.getDiscountPercent() == null) {
            pkg.setDiscountPercent(0.0);
        }

        if (pkg.getType() == PackageType.MEMBERSHIP) {
            pkg.setPriorityLevel(0);
            if (pkg.getDurationDays() == null) {
                pkg.setDurationDays(0);
            }
        }

        if (pkg.getType() == PackageType.ROOM_PROMOTION) {
            pkg.setQuotaLimit(0);
        }
    }
}