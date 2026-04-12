package com.homeverse.property.kafka.producer;

import com.homeverse.property.entity.OutboxEvent;
import com.homeverse.property.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxMessageRelay {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // Quét mỗi 2 giây (2000 milliseconds)
    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void relayMessagesToKafka() {
        // Tìm các tin nhắn đang chờ gửi
        List<OutboxEvent> pendingEvents = outboxRepository.findByStatus("PENDING");

        if (!pendingEvents.isEmpty()) {
            log.info("Outbox Relay: Tìm thấy {} tin nhắn đang chờ gửi", pendingEvents.size());
        }

        for (OutboxEvent event : pendingEvents) {
            try {
                // Gửi qua Kafka (.get() để đảm bảo chờ Kafka xác nhận đã nhận thành công)
                kafkaTemplate.send(event.getTopic(), event.getPayload()).get();

                // Gửi thành công -> Cập nhật trạng thái
                event.setStatus("COMPLETED");
                outboxRepository.save(event);

                log.info("Outbox Relay: Gửi thành công Event ID {} lên topic {}", event.getId(), event.getTopic());
            } catch (Exception e) {
                // Nếu Kafka sập mạng, nó in lỗi ra, vòng lặp sau sẽ quét lại và gửi lại
                log.error("Outbox Relay: Gửi thất bại Event ID {}. Lỗi: {}", event.getId(), e.getMessage());
            }
        }
    }
}