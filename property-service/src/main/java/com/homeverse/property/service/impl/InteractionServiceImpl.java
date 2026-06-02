package com.homeverse.property.service.impl;

import com.homeverse.property.dto.response.InteractionPropertyDTO;
import com.homeverse.property.entity.Property;
import com.homeverse.property.entity.UserPropertyInteraction;
import com.homeverse.property.entity.UserPropertyInteraction.InteractionType;
import com.homeverse.property.repository.InteractionRepository;
import com.homeverse.property.service.InteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionServiceImpl implements InteractionService {

    private final InteractionRepository interactionRepository;
    private final StringRedisTemplate redisTemplate;

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

    private boolean handleToggle(Long userId, String guestId, Long propertyId, InteractionType type,
            String redisKeySuffix) {
        var existing = interactionRepository.findInteraction(userId, guestId, propertyId, type);
        String redisKey = "property:" + propertyId + ":" + redisKeySuffix;

        if (existing.isPresent()) {
            interactionRepository.delete(existing.get());

            Long current = redisTemplate.opsForValue().decrement(redisKey);

            if (current == null || current < 0) {
                redisTemplate.opsForValue().set(redisKey, "0");
            }

            return false;
        } else {
            interactionRepository.save(UserPropertyInteraction.builder()
                    .userId(userId)
                    .guestId(guestId)
                    .propertyId(propertyId)
                    .interactionType(type)
                    .build());

            redisTemplate.opsForValue().increment(redisKey);
            return true;
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
    public void trackView(Long userId, String guestId, Long propertyId) {

    redisTemplate.opsForValue()
            .increment("property:" + propertyId + ":views");

    log.info(
            "View tracked: property={}, user={}, guest={}",
            propertyId,
            userId,
            guestId
    );
}
}