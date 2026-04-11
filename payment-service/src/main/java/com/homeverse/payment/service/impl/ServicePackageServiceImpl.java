package com.homeverse.payment.service.impl;

import com.homeverse.payment.entity.Transaction;
import com.homeverse.payment.repository.TransactionRepository;
import com.homeverse.payment.service.ServicePackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ServicePackageServiceImpl implements ServicePackageService {

    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public void buyMembership(Long userId, Long packageId) {
        // Trong Microservices, việc trừ tiền ví và cập nhật hạng nên xử lý ở IdentityService
        // PaymentService chỉ nên đóng vai trò ghi nhận giao dịch.
        
        // Giả sử giá gói được lấy từ DB hoặc từ DTO truyền vào (ở đây mình ví dụ fix cứng để build được)
        BigDecimal packagePrice = BigDecimal.valueOf(100000); 

        // 1. Lưu lịch sử giao dịch (Sử dụng cấu trúc Entity Transaction Ân vừa sửa)
        Transaction trans = Transaction.builder()
                .userId(userId) // Dùng userId thay vì object User
                .amount(packagePrice)
                .type("BUY_MEMBERSHIP")
                .description("Nâng cấp gói hội viên ID: " + packageId)
                .status("SUCCESS")
                .createdAt(LocalDateTime.now())
                .build();
        
        transactionRepository.save(trans);

        // 2. Gửi thông báo: Nên bắn tin nhắn qua Kafka để NotificationService nhận
        // log.info("Đã ghi nhận giao dịch nâng cấp gói cho user: {}", userId);
    }
}