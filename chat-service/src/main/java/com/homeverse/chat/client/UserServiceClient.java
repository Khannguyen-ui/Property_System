package com.homeverse.chat.client;
import com.homeverse.chat.dto.UserSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service") // Tên service trong docker-compose
public interface UserServiceClient {
    
    @GetMapping("/customers/{id}/summary")
    UserSummaryDTO getUserSummary(@PathVariable("id") Long id);
}