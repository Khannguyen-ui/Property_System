package com.homeverse.wallet.controller;

import com.homeverse.wallet.dto.HoldRequest;
import com.homeverse.wallet.dto.ReleaseRequest;
import com.homeverse.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    // 1. Lấy số dư ví hiện tại (Dùng JWT trả về userId dạng String)
    @GetMapping("/me")
    public ResponseEntity<?> getMyWallet(@AuthenticationPrincipal String userId) {
        // Ép kiểu String từ Token sang Long để gọi Service
        return ResponseEntity.ok(walletService.getWalletByUserId(Long.valueOf(userId)));
    }

    // 2. Lấy lịch sử giao dịch (Phân trang)
    @GetMapping("/transactions")
    public ResponseEntity<?> getMyTransactions(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(walletService.getTransactions(Long.valueOf(userId), page, size));
    }

    // 3. Hold tiền đặt cọc (Dùng Long userId từ DTO)
    @PostMapping("/hold")
    public ResponseEntity<?> holdMoney(@RequestBody HoldRequest request) {
        walletService.holdMoney(request.getUserId(), request.getAmount(), request.getReferenceId());
        return ResponseEntity.ok("Đã giữ tiền cọc thành công!");
    }

    // 4. Giải phóng hold - Hoàn cọc
    @PostMapping("/release")
    public ResponseEntity<?> releaseMoney(@RequestBody ReleaseRequest request) {
        walletService.releaseMoney(request.getUserId(), request.getAmount(), request.getReferenceId());
        return ResponseEntity.ok("Đã hoàn tiền cọc!");
    }
    @PostMapping("/debit")
    public ResponseEntity<?> debit(
            @RequestParam Long userId, 
            @RequestParam java.math.BigDecimal amount) {
        walletService.debit(userId, amount);
        return ResponseEntity.ok("Đã trừ tiền thành công!");
    }
}