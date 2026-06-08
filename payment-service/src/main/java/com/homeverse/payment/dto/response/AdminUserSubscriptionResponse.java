package com.homeverse.payment.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserSubscriptionResponse {

    private MembershipInfo membership;
    private List<PromotionInfo> activePromotions;
    private List<TransactionInfo> recentTransactions;
    private List<MembershipInfo> subscriptionHistory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MembershipInfo {
        private Long subscriptionId;
        private Long packageId;
        private String packageName;
        private String packageType;
        private BigDecimal amount;
        private LocalDateTime startedAt;
        private LocalDateTime expiresAt;
        private Boolean active;
        private Integer quotaLimit;
        private String sourceNote;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromotionInfo {
        private Long transactionId;
        private String packageName;
        private String packageType;
        private BigDecimal amount;
        private LocalDateTime purchasedAt;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionInfo {
        private Long id;
        private BigDecimal amount;
        private String type;
        private String description;
        private String status;
        private LocalDateTime createdAt;
    }
}