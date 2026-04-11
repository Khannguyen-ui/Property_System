package com.homeverse.identity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.core.env.Environment;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication(scanBasePackages = {
        "com.homeverse.identity",
        "com.homeverse.common"
})
@EntityScan(basePackages = {"com.homeverse.identity.entity", "com.homeverse.common.entity"})
@EnableJpaAuditing
@EnableFeignClients
@Slf4j // Thêm cái này để dùng log.info, log.error...
public class IdentityServiceApplication {

    public static void main(String[] args) throws UnknownHostException {
        SpringApplication app = new SpringApplication(IdentityServiceApplication.class);
        Environment env = app.run(args).getEnvironment();

        // Log ra thông tin rực rỡ khi khởi động thành công
        String protocol = "http";
        if (env.getProperty("server.ssl.key-store") != null) {
            protocol = "https";
        }

        log.info("\n----------------------------------------------------------\n\t" +
                        "Ứng dụng '{}' đang chạy!\n\t" +
                        "Truy cập tại: \n\t" +
                        "Local: \t\t{}://localhost:{}\n\t" +
                        "External: \t{}://{}:{}\n\t" +
                        "Profile(s): \t{}\n" +
                        "----------------------------------------------------------",
                env.getProperty("spring.application.name"),
                protocol,
                env.getProperty("server.port"),
                protocol,
                InetAddress.getLocalHost().getHostAddress(),
                env.getProperty("server.port"),
                env.getActiveProfiles());
    }
}