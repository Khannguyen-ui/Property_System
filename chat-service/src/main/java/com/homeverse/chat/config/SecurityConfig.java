package com.homeverse.chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import jakarta.servlet.http.HttpServletResponse;
import javax.crypto.spec.SecretKeySpec;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    public SecurityConfig() {
        System.out.println(">>>> CHAT SERVICE ĐÃ LOAD SECURITY CONFIG THÀNH CÔNG!!!!");
    }
    @Value("${jwt.secret}")
    private String secretKey;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/ws-chat/**").permitAll()
                .requestMatchers("/api/chat/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder())) // Ép dùng Decoder tự định nghĩa
                .authenticationEntryPoint((request, response, authException) -> {
                    // In lỗi ra Log để biết tại sao nó đuổi mình ra
                    System.out.println("Chat Service chặn: " + authException.getMessage());
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
                })
            );

        return http.build();
    }

   @Bean
public JwtDecoder jwtDecoder() {
    // 1. Tạo SecretKeySpec và chỉ định thuật toán là HMAC
    SecretKeySpec secretKeySpec = new SecretKeySpec(
        this.secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), 
        "HmacSHA256"
    );

    // 2. Ép NimbusJwtDecoder phải dùng thuật toán HS256 để giải mã
    return NimbusJwtDecoder.withSecretKey(secretKeySpec)
            .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
            .build();
}
}