package com.homeverse.property.scheduling;

import com.homeverse.property.entity.Property;
import com.homeverse.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PropertyExpirationTask {

    private final PropertyRepository propertyRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireProperties() {
        LocalDateTime now = LocalDateTime.now();

        List<Property> expiredProperties = propertyRepository
                .findByStatusAndExpiresAtBefore(Property.Status.ACTIVE, now);

        if (expiredProperties.isEmpty()) {
            return;
        }

        for (Property property : expiredProperties) {
            property.setStatus(Property.Status.EXPIRED);
            property.setIsPromoted(false);
            property.setPromotionPackageId(null);
            property.setPromotionPackageName(null);
            property.setPromotionExpiresAt(null);
        }

        propertyRepository.saveAll(expiredProperties);

        log.info("Đã chuyển {} bài đăng hết hạn ", expiredProperties.size());
    }
}