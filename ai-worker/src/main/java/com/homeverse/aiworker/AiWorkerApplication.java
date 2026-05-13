package com.homeverse.aiworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AiWorkerApplication {
    public static void main(String[] args) {

        SpringApplication.run(AiWorkerApplication.class, args);
        System.out.println("🚀 [AI Worker Service] Đã khởi động thành công và đang hóng Kafka!");
    }
}