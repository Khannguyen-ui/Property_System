package com.homeverse.property.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeverse.property.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentNotificationProducer {

    private static final String TOPIC = "notification-topic";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void send(NotificationEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, message);

            log.info(" Sent appointment notification: {}", message);
        } catch (Exception e) {
            log.error(" Failed to send appointment notification", e);
        }
    }
}