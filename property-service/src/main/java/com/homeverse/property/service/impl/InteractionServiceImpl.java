package com.homeverse.property.service.impl;

import com.homeverse.common.dto.NotificationEvent;
import com.homeverse.property.config.RecommendClient;
import com.homeverse.property.dto.request.TrackEventRequest;
import com.homeverse.property.dto.response.InteractionPropertyDTO;
import com.homeverse.property.dto.response.UserInterestProfileDTO;
import com.homeverse.property.entity.Property;
import com.homeverse.property.entity.PropertyContact;
import com.homeverse.property.entity.UserPropertyInteraction;
import com.homeverse.property.entity.UserPropertyInteraction.InteractionType;
import com.homeverse.property.repository.InteractionRepository;
import com.homeverse.property.repository.PropertyContactRepository;
import com.homeverse.property.repository.PropertyRepository;
import com.homeverse.property.service.FeatureCalculator;
import com.homeverse.property.service.InteractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionServiceImpl implements InteractionService {

    private final InteractionRepository interactionRepository;
    private final StringRedisTemplate redisTemplate;
    private final RecommendClient recommendClient;
    private final PropertyRepository propertyRepository;
    private final PropertyContactRepository propertyContactRepository;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final FeatureCalculator featureCalculator;

    @Override
    @Transactional
    public boolean toggleLike(Long userId, String guestId, Long propertyId) {
        return handleToggle(userId, guestId, propertyId, InteractionType.LIKE, "likes");
    }

    @Override
    @Transactional
    public boolean toggleSave(Long userId, String guestId, Long propertyId) {
        return handleToggle(userId, guestId, propertyId, InteractionType.SAVE, "saves");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InteractionPropertyDTO> getLikedProperties(Long userId, String guestId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        return interactionRepository
                .findPropertiesByInteraction(userId, guestId, InteractionType.LIKE, Property.Status.ACTIVE, pageable)
                .map(property -> toInteractionPropertyDTO(property, true, isSaved(userId, guestId, property.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InteractionPropertyDTO> getSavedProperties(Long userId, String guestId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        return interactionRepository
                .findPropertiesByInteraction(userId, guestId, InteractionType.SAVE, Property.Status.ACTIVE, pageable)
                .map(property -> toInteractionPropertyDTO(property, isLiked(userId, guestId, property.getId()), true));
    }

    private boolean handleToggle(Long userId, String guestId, Long propertyId, InteractionType type, String redisKeySuffix) {
        var existing = interactionRepository.findInteraction(userId, guestId, propertyId, type);
        String redisKey = "property:" + propertyId + ":" + redisKeySuffix;

        if (existing.isPresent()) {
            interactionRepository.delete(existing.get());

            Long current = redisTemplate.opsForValue().decrement(redisKey);
            if (current == null || current < 0) {
                redisTemplate.opsForValue().set(redisKey, "0");
            }

            return false;
        }

        interactionRepository.save(UserPropertyInteraction.builder()
                .userId(userId)
                .guestId(guestId)
                .propertyId(propertyId)
                .interactionType(type)
                .build());

        redisTemplate.opsForValue().increment(redisKey);

        trackRecommendation(userId, propertyId, type.name());

        return true;
    }

private void trackRecommendation(Long userId, Long propertyId, String action) {
    if (userId == null) {
        return;
    }

    try {
        Property property = propertyRepository.findById(propertyId).orElseThrow();
        UserInterestProfileDTO profile = recommendClient.getProfile(userId);

        TrackEventRequest request = featureCalculator.buildTrackRequest(
                userId,
                property,
                profile,
                action
        );
    log.info(
        "SEND TO RECOMMEND => userId={}, itemId={}, action={}, provinceMatch={}, districtMatch={}, wardMatch={}, streetMatch={}, locationMatch={}, categoryMatch={}",
        request.getUserId(),
        request.getItemId(),
        request.getAction(),
        request.getProvinceMatch(),
        request.getDistrictMatch(),
        request.getWardMatch(),
        request.getStreetMatch(),
        request.getLocationMatch(),
        request.getCategoryMatch()
);

        log.info(
    "Property {} => province={}, district={}, ward={}, street={}",
    property.getId(),
    property.getProvince(),
    property.getDistrict(),
    property.getWard(),
    property.getStreet()
);

        recommendClient.track(request);

    } catch (Exception e) {
        log.warn("Track {} failed userId={}, propertyId={}: {}", action, userId, propertyId, e.getMessage());
    }
}

    private boolean isLiked(Long userId, String guestId, Long propertyId) {
        return interactionRepository
                .findInteraction(userId, guestId, propertyId, InteractionType.LIKE)
                .isPresent();
    }

    private boolean isSaved(Long userId, String guestId, Long propertyId) {
        return interactionRepository
                .findInteraction(userId, guestId, propertyId, InteractionType.SAVE)
                .isPresent();
    }

    private InteractionPropertyDTO toInteractionPropertyDTO(Property property, boolean liked, boolean saved) {
        return InteractionPropertyDTO.builder()
                .id(property.getId())
                .title(property.getTitle())
                .price(property.getPrice())
                .province(property.getProvince())
                .district(property.getDistrict())
                .address(property.getAddress())
                .propertyType(property.getPropertyType() == null ? null : property.getPropertyType().name())
                .transactionType(property.getTransactionType() == null ? null : property.getTransactionType().name())
                .imageUrl(getFirstImage(property))
                .createdAt(property.getCreatedAt())
                .liked(liked)
                .saved(saved)
                .build();
    }

    private String getFirstImage(Property property) {
        if (property.getImages() == null || property.getImages().isEmpty()) {
            return null;
        }

        return property.getImages()
                .stream()
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(null);
    }

   @Override
public void trackView(Long userId, String guestId, Long propertyId, Double watchTime, Double duration) {
    redisTemplate.opsForValue().increment("property:" + propertyId + ":views");

    if (userId == null) return;

    try {
        Property property = propertyRepository.findById(propertyId).orElseThrow();
        UserInterestProfileDTO profile = recommendClient.getProfile(userId);

        TrackEventRequest event = featureCalculator.buildTrackRequest(
                userId,
                property,
                profile,
                "VIEW"
        );

        event.setWatchTime(watchTime != null ? watchTime : 0.0);
        event.setDuration(duration != null && duration > 0 ? duration : 1.0);

        recommendClient.track(event);
    } catch (Exception e) {
        log.warn("Track VIEW failed userId={}, propertyId={}: {}", userId, propertyId, e.getMessage());
    }
}

    @Override
    public void shareProperty(Long userId, Long propertyId) {
        propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài đăng"));

        redisTemplate.opsForValue().increment("property:" + propertyId + ":shares");

        trackRecommendation(userId, propertyId, "SHARE");
    }

    @Override
    public void trackClick(Long userId, String guestId, Long propertyId) {
        redisTemplate.opsForValue().increment("property:" + propertyId + ":clicks");

        log.info("Click tracked: property={}, user={}, guest={}", propertyId, userId, guestId);

        trackRecommendation(userId, propertyId, "CLICK");
    }

    @Override
    public void contactProperty(Long userId, Long propertyId) {
        log.info("CONTACT API HIT userId={}, propertyId={}", userId, propertyId);

        Property property = propertyRepository.findById(propertyId).orElseThrow();

        redisTemplate.opsForValue().increment("property:" + propertyId + ":contacts");

        sendContactNotification(userId, property);

        if (!propertyContactRepository.existsByUserIdAndPropertyId(userId, propertyId)) {
            propertyContactRepository.save(
                    PropertyContact.builder()
                            .userId(userId)
                            .ownerId(property.getOwnerId())
                            .propertyId(propertyId)
                            .build()
            );
        }

        trackRecommendation(userId, propertyId, "CONTACT");
    }

    private void sendContactNotification(Long userId, Property property) {
        if (userId == null || property == null || property.getOwnerId() == null) {
            return;
        }

        if (property.getOwnerId().equals(userId)) {
            return;
        }

        try {
            NotificationEvent event = NotificationEvent.builder()
                    .receiverId(property.getOwnerId())
                    .title("Có người liên hệ")
                    .content("Có người vừa bấm liên hệ bài đăng của bạn")
                    .type("PROPERTY_CONTACT")
                    .referenceId(property.getId())
                    .build();

            kafkaTemplate.send("notification-topic", event);
        } catch (Exception e) {
            log.warn("Send contact notification failed propertyId={}, userId={}: {}",
                    property.getId(), userId, e.getMessage());
        }
    }
}