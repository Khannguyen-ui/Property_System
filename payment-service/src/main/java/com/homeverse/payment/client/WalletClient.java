package com.homeverse.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;


@FeignClient(name = "wallet-service", url = "http://wallet-service:8089")
public interface WalletClient {

    @PostMapping("/api/wallets/debit") // Đường dẫn này phải khớp với Controller bên Wallet Service
    void debit(@RequestParam("userId") Long userId, @RequestParam("amount") BigDecimal amount);
}