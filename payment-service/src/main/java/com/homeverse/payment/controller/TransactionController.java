package com.homeverse.payment.controller;

import com.homeverse.payment.entity.Transaction;
import com.homeverse.payment.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<?> purchasePackage(@RequestParam Long userId, @RequestParam Long packageId){
        return ResponseEntity.ok("Giao dịch đang được xử lý qua Identity Service...");
    }
}