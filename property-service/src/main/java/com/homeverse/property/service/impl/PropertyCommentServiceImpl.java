package com.homeverse.property.service.impl;

import com.homeverse.property.config.RecommendClient;
import com.homeverse.property.dto.request.CommentRequest;
import com.homeverse.property.dto.request.TrackEventRequest;
import com.homeverse.property.dto.response.CommentResponse;
import com.homeverse.property.entity.Property;
import com.homeverse.property.entity.PropertyComment;
import com.homeverse.property.repository.PropertyCommentRepository;
import com.homeverse.property.repository.PropertyRepository;
import com.homeverse.property.service.PropertyCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.kafka.core.KafkaTemplate;
import com.homeverse.common.dto.NotificationEvent;
import com.homeverse.property.entity.Property;

@Service
@RequiredArgsConstructor
public class PropertyCommentServiceImpl implements PropertyCommentService {

    private final PropertyCommentRepository commentRepository;
    private final StringRedisTemplate redisTemplate;
    private final RecommendClient recommendClient;
    private final PropertyRepository propertyRepository;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Override
    @Transactional
    public CommentResponse createComment(Long userId, String guestId, CommentRequest request) {
        if (userId == null && (guestId == null || guestId.isBlank())) {
            throw new RuntimeException("User hoặc guestId không hợp lệ");
        }

        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new RuntimeException("Nội dung bình luận không được trống");
        }

        Long parentId = request.getParentId();
        Long replyToUserId = request.getReplyToUserId();

        if (parentId != null) {
            PropertyComment parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy comment cha"));

            if (parent.getParentId() != null) {
                parentId = parent.getParentId();
            }

            if (replyToUserId == null) {
                replyToUserId = parent.getUserId();
            }
        }

        PropertyComment comment = PropertyComment.builder()
                .propertyId(request.getPropertyId())
                .userId(userId)
                .guestId(guestId)
                .parentId(parentId)
                .replyToUserId(replyToUserId)
                .content(request.getContent().trim())
                .build();

        PropertyComment saved = commentRepository.save(comment);

        redisTemplate.opsForValue()
                .increment("property:" + request.getPropertyId() + ":comments");

        trackComment(userId, request.getPropertyId());

        sendCommentNotification(userId, request.getPropertyId());

        return toResponse(saved);
    }
    private void sendCommentNotification(Long userId, Long propertyId) {
    if (userId == null || propertyId == null) {
        return;
    }

    try {
        Property property = propertyRepository.findById(propertyId)
                .orElse(null);

        if (property == null) {
            return;
        }

        if (property.getOwnerId() == null) {
            return;
        }

        if (property.getOwnerId().equals(userId)) {
            return;
        }

        NotificationEvent event = NotificationEvent.builder()
                .receiverId(property.getOwnerId())
                .title("Bình luận mới")
                .content("Có người vừa bình luận bài đăng của bạn")
                .type("COMMENT_NEW")
                .referenceId(propertyId)
                .build();

        kafkaTemplate.send("notification-topic", event);

    } catch (Exception e) {
        e.printStackTrace();
    }
}

    private void trackComment(Long userId, Long propertyId) {

        System.out.println("TRACK COMMENT CALLED userId=" + userId +
                ", propertyId=" + propertyId);

        if (userId == null) {
            System.out.println("TRACK COMMENT SKIPPED: userId null");
            return;
        }

        try {

            Property property = propertyRepository.findById(propertyId)
                    .orElse(null);

            if (property == null) {
                System.out.println("TRACK COMMENT SKIPPED: property not found");
                return;
            }

            System.out.println("BEFORE RECOMMEND TRACK");

            recommendClient.track(
                    TrackEventRequest.builder()
                            .userId(userId)
                            .itemId(propertyId)
                            .itemType(
                                    property.getVideoUrl() != null
                                            && !property.getVideoUrl().isBlank()
                                                    ? "reel"
                                                    : "property")
                            .action("COMMENT")
                            .watchTime(0.0)
                            .duration(1.0)
                            .price(
                                    property.getPrice() != null
                                            ? property.getPrice().doubleValue()
                                            : 0.0)
                            .userBudget(
                                    property.getPrice() != null
                                            ? property.getPrice().doubleValue()
                                            : 0.0)
                            .locationMatch(
                                    property.getDistrict() != null
                                            ? 1
                                            : 0)
                            .categoryMatch(
                                    property.getPropertyType() != null
                                            ? 1
                                            : 0)
                            .district(property.getDistrict())
                            .build());

            System.out.println("AFTER RECOMMEND TRACK");

        } catch (Exception e) {
            System.out.println("TRACK COMMENT ERROR = " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(Long propertyId, int page, int size) {
        return commentRepository
                .findByPropertyIdAndParentIdIsNullAndStatusOrderByCreatedAtDesc(
                        propertyId,
                        PropertyComment.Status.ACTIVE,
                        PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getReplies(Long parentId, int page, int size) {
        return commentRepository
                .findByParentIdAndStatusOrderByCreatedAtAsc(
                        parentId,
                        PropertyComment.Status.ACTIVE,
                        PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        PropertyComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bình luận"));

        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa bình luận này");
        }

        int deletedReplies = 0;

        if (comment.getParentId() == null) {
            deletedReplies = commentRepository.softDeleteRepliesByParentId(commentId);
        }

        comment.setStatus(PropertyComment.Status.DELETED);
        commentRepository.save(comment);

        long totalDeleted = 1 + deletedReplies;

        redisTemplate.opsForValue()
                .decrement("property:" + comment.getPropertyId() + ":comments", totalDeleted);
    }

    @Override
    public long countComments(Long propertyId) {
        String key = "property:" + propertyId + ":comments";
        String value = redisTemplate.opsForValue().get(key);

        if (value != null) {
            return Long.parseLong(value);
        }

        long count = commentRepository.countByPropertyIdAndStatus(
                propertyId,
                PropertyComment.Status.ACTIVE);

        redisTemplate.opsForValue().set(key, String.valueOf(count));

        return count;
    }

    private CommentResponse toResponse(PropertyComment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .propertyId(comment.getPropertyId())
                .userId(comment.getUserId())
                .guestId(comment.getGuestId())
                .parentId(comment.getParentId())
                .replyToUserId(comment.getReplyToUserId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}