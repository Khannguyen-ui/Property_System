package com.homeverse.property.config;

import com.homeverse.property.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)   // ← BẮT BUỘC để @PreAuthorize hoạt động
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // ================== PUBLIC ==================
                        // Ai cũng xem được danh sách dự án & bất động sản
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/properties/**", "/projects/**","/amenities/**","/public/**","/owners/**").permitAll()

                        // ================== ADMIN ==================
                        // Tất cả request dưới /admin/** phải là ADMIN (URL level protection)
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // ================== OWNER / USER ==================
                        // Các API còn lại (create/update/delete property...) yêu cầu đã login
                        .anyRequest().authenticated()
                )

                // Đưa JwtAuthenticationFilter vào pipeline
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}