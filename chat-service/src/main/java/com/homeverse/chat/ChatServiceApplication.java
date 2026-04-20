package com.homeverse.chat;

import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;

@SpringBootApplication(scanBasePackages = {
    "com.homeverse.chat", 
    "com.homeverse.common"
})
@EntityScan(basePackages = {
    "com.homeverse.chat.entity", 
    "com.homeverse.common.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.homeverse.chat.repository"
})
@EnableFeignClients(basePackages = "com.homeverse.chat.client")
@EnableJpaAuditing
public class ChatServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }

    // THÊM DÒNG NÀY ĐỂ FIX LỖI ChatMapper
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}