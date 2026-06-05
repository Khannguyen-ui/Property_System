package com.homeverse.property.service.impl;

import com.homeverse.common.dto.NotificationEvent;
import com.homeverse.property.config.RecommendClient;
import com.homeverse.property.dto.request.TrackEventRequest;
import com.homeverse.property.dto.response.OwnerFollowResponse;
import com.homeverse.property.entity.OwnerFollow;
import com.homeverse.property.repository.OwnerFollowRepository;
import com.homeverse.property.service.OwnerFollowService;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OwnerFollowServiceImpl implements OwnerFollowService {

    private final OwnerFollowRepository ownerFollowRepository;
    private final StringRedisTemplate redisTemplate;
    private final RecommendClient recommendClient;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    @Override
    @Transactional
    public OwnerFollowResponse toggleFollow(Long followerId, Long ownerId) {
        if (followerId == null) {
            throw new RuntimeException("Bạn cần đăng nhập để follow");
        }

        if (ownerId == null) {
            throw new RuntimeException("ownerId không hợp lệ");
        }

        if (followerId.equals(ownerId)) {
            throw new RuntimeException("Bạn không thể follow chính mình");
        }

        String countKey = "owner:" + ownerId + ":followers";

        var existing = ownerFollowRepository.findByFollowerIdAndOwnerId(followerId, ownerId);

        boolean followed;

        if (existing.isPresent()) {
            ownerFollowRepository.delete(existing.get());

            Long current = redisTemplate.opsForValue().decrement(countKey);

            if (current == null || current < 0) {
                long dbCount = ownerFollowRepository.countByOwnerId(ownerId);
                redisTemplate.opsForValue().set(countKey, String.valueOf(dbCount));
            }

            followed = false;
        } else {
            ownerFollowRepository.save(
                    OwnerFollow.builder()
                            .followerId(followerId)
                            .ownerId(ownerId)
                            .build());

            redisTemplate.opsForValue().increment(countKey);

            trackFollowOwner(followerId, ownerId);

            followed = true;
            sendFollowNotification(followerId, ownerId);

            followed = true;
        }

        return OwnerFollowResponse.builder()
                .ownerId(ownerId)
                .followerId(followerId)
                .followed(followed)
                .followerCount(countFollowers(ownerId))
                .build();
    }
    private void sendFollowNotification(
        Long followerId,
        Long ownerId
) {
    try {

        NotificationEvent event = NotificationEvent.builder()
                .receiverId(ownerId)
                .senderId(followerId)
                .title("Người theo dõi mới")
                .content("Có người vừa theo dõi bạn")
                .type("OWNER_FOLLOWED")
                .referenceId(followerId)
                .build();

        kafkaTemplate.send(
                "notification-topic",
                event
        );

    } catch (Exception e) {
        e.printStackTrace();
    }
}

    @Override
    @Transactional(readOnly = true)
    public boolean isFollowing(Long followerId, Long ownerId) {
        if (followerId == null || ownerId == null) {
            return false;
        }

        return ownerFollowRepository.existsByFollowerIdAndOwnerId(followerId, ownerId);
    }

    @Override
    public long countFollowers(Long ownerId) {
        String key = "owner:" + ownerId + ":followers";

        String value = redisTemplate.opsForValue().get(key);

        if (value != null) {
            return Long.parseLong(value);
        }

        long count = ownerFollowRepository.countByOwnerId(ownerId);

        redisTemplate.opsForValue().set(key, String.valueOf(count));

        return count;
    }

    @Override
    public long countFollowing(Long followerId) {
        return ownerFollowRepository.countByFollowerId(followerId);
    }

    private void trackFollowOwner(Long followerId, Long ownerId) {
        try {
            recommendClient.track(
                    TrackEventRequest.builder()
                            .userId(followerId)
                            .itemId(ownerId)
                            .itemType("owner")
                            .action("FOLLOW_OWNER")
                            .watchTime(0.0)
                            .duration(1.0)
                            .price(0.0)
                            .userBudget(0.0)
                            .locationMatch(0)
                            .categoryMatch(0)
                            .district(null)
                            .build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Long> getFollowedOwnerIds(Long followerId) {
        if (followerId == null) {
            return List.of();
        }

        return ownerFollowRepository.findOwnerIdsByFollowerId(followerId);
    }
}