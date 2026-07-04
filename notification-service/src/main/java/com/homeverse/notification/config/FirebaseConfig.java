package com.homeverse.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.config.path:}")
    private String firebasePath;

    @PostConstruct
    public void init() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                return;
            }

            if (firebasePath == null || firebasePath.isBlank()) {
                log.warn("FIREBASE_CONFIG_PATH chưa được cấu hình. FCM disabled.");
                return;
            }

            File file = new File(firebasePath);
            if (!file.exists()) {
                log.warn("Firebase service account không tồn tại tại path: {}. FCM disabled.", firebasePath);
                return;
            }

            try (FileInputStream serviceAccount = new FileInputStream(file)) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully from {}", firebasePath);
            }

        } catch (Exception e) {
            log.error("Failed to initialize Firebase. FCM disabled: {}", e.getMessage());
        }
    }
}