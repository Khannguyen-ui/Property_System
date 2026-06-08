package com.homeverse.payment.controller;

import com.homeverse.payment.dto.response.AdminUserSubscriptionResponse;
import com.homeverse.payment.entity.Transaction;
import com.homeverse.payment.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository transactionRepository;

    // Sửa lại: Lấy lịch sử theo userId truyền từ Frontend hoặc Token đã parse
    @GetMapping("/my-history/{userId}")
    public ResponseEntity<List<Transaction>> getMyHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(transactionRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @PostMapping("/purchase-package")
    public ResponseEntity<?> purchasePackage(@RequestParam Long userId, @RequestParam Long packageId) {
        return ResponseEntity.ok("Giao dịch đang được xử lý qua Identity Service...");
    }

    @GetMapping("/all")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(
                transactionRepository.findAllByOrderByCreatedAtDesc());
    }
    @GetMapping("/admin/users/{userId}/subscriptions")
    public ResponseEntity<AdminUserSubscriptionResponse> getUserSubscriptions(
            @PathVariable Long userId
    ) {
        List<Transaction> transactions =
                transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);

        LocalDateTime now = LocalDateTime.now();

        Transaction latestMembership = transactions.stream()
                .filter(t -> "SUCCESS".equalsIgnoreCase(t.getStatus()))
                .filter(t -> {
                    String type = String.valueOf(t.getType()).toUpperCase();
                    return type.contains("MEMBERSHIP")
                            || type.contains("PACKAGE");
                })
                .max(Comparator.comparing(Transaction::getCreatedAt))
                .orElse(null);

        AdminUserSubscriptionResponse.MembershipInfo membership = null;

        if (latestMembership != null) {
            LocalDateTime estimatedExpiresAt =
                    latestMembership.getCreatedAt() != null
                            ? latestMembership.getCreatedAt().plusDays(30)
                            : null;

            membership = AdminUserSubscriptionResponse.MembershipInfo.builder()
                    .packageName(extractPackageName(latestMembership.getDescription()))
                    .packageType(latestMembership.getType())
                    .amount(latestMembership.getAmount())
                    .purchasedAt(latestMembership.getCreatedAt())
                    .estimatedExpiresAt(estimatedExpiresAt)
                    .active(estimatedExpiresAt != null && estimatedExpiresAt.isAfter(now))
                    .sourceNote("Tạm tính từ giao dịch gần nhất. Nên bổ sung bảng user_subscriptions để chính xác tuyệt đối.")
                    .build();
        }

        List<AdminUserSubscriptionResponse.PromotionInfo> activePromotions =
                transactions.stream()
                        .filter(t -> "SUCCESS".equalsIgnoreCase(t.getStatus()))
                        .filter(t -> {
                            String type = String.valueOf(t.getType()).toUpperCase();
                            String desc = String.valueOf(t.getDescription()).toUpperCase();
                            return type.contains("PROMOTION")
                                    || type.contains("PUSH")
                                    || desc.contains("ĐẨY")
                                    || desc.contains("VIP");
                        })
                        .limit(20)
                        .map(t -> AdminUserSubscriptionResponse.PromotionInfo.builder()
                                .transactionId(t.getId())
                                .packageName(extractPackageName(t.getDescription()))
                                .packageType(t.getType())
                                .amount(t.getAmount())
                                .purchasedAt(t.getCreatedAt())
                                .description(t.getDescription())
                                .build())
                        .toList();

        List<AdminUserSubscriptionResponse.TransactionInfo> recentTransactions =
                transactions.stream()
                        .limit(20)
                        .map(t -> AdminUserSubscriptionResponse.TransactionInfo.builder()
                                .id(t.getId())
                                .amount(t.getAmount())
                                .type(t.getType())
                                .description(t.getDescription())
                                .status(t.getStatus())
                                .createdAt(t.getCreatedAt())
                                .build())
                        .toList();

        return ResponseEntity.ok(
                AdminUserSubscriptionResponse.builder()
                        .membership(membership)
                        .activePromotions(activePromotions)
                        .recentTransactions(recentTransactions)
                        .build()
        );
    }

    private String extractPackageName(String description) {
        if (description == null || description.isBlank()) {
            return "Không xác định";
        }

        String marker = "Mua gói:";
        if (description.contains(marker)) {
            return description.substring(description.indexOf(marker) + marker.length()).trim();
        }

        return description;
    }
}