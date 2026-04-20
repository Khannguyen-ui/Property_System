package com.homeverse.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;

// Name phải khớp với name service trong docker-compose, url trỏ tới port nội bộ của nó
@FeignClient(name = "wallet-service", url = "http://wallet-service:8088")
public interface WalletClient {

    @PostMapping("/api/wallets/debit") // Đường dẫn này phải khớp với Controller bên Wallet Service
    void debit(@RequestParam("userId") Long userId, @RequestParam("amount") BigDecimal amount);
}