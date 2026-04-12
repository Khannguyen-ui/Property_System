package com.homeverse.property.service.impl;

import com.homeverse.property.entity.UserPropertyInteraction;
import com.homeverse.property.entity.UserPropertyInteraction.InteractionType;
import com.homeverse.property.repository.InteractionRepository;
import com.homeverse.property.service.InteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private boolean handleToggle(Long userId, String guestId, Long propertyId, InteractionType type, String redisKeySuffix) {
        var existing = interactionRepository.findInteraction(userId, guestId, propertyId, type);
        String redisKey = "property:" + propertyId + ":" + redisKeySuffix;

        if (existing.isPresent()) {
            // ĐÃ CÓ -> THỰC HIỆN UNLIKE / UNSAVE
            interactionRepository.delete(existing.get());
            redisTemplate.opsForValue().decrement(redisKey);
            return false; // Trả về false nghĩa là đã gỡ bỏ
        } else {
            // CHƯA CÓ -> THỰC HIỆN LIKE / SAVE
            interactionRepository.save(UserPropertyInteraction.builder()
                    .userId(userId)
                    .guestId(guestId)
                    .interactionType(type)
                    .build());
            redisTemplate.opsForValue().increment(redisKey);
            return true; // Trả về true nghĩa là đã thêm
        }
    }
}