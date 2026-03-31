package com.homeverse.identity.config;

import com.homeverse.identity.entity.UserCredential;
import com.homeverse.identity.repository.UserCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserCredentialRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByEmail("admin@homeverse.com").isEmpty()) {

            UserCredential admin = UserCredential.builder()
                    .email("admin@homeverse.com")
                    .password(passwordEncoder.encode("Admin@123456"))
                    .fullName("System Administrator")
                    .phone("0999999999")
                    .role(UserCredential.Role.ADMIN)
                    .isActive(true)
                    .kycStatus("VERIFIED")
                    .build();

            userRepository.save(admin);

        }
    }
}