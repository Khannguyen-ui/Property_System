package com.homeverse.chat.service.impl;

import com.homeverse.chat.client.UserServiceClient;
import com.homeverse.chat.dto.NotificationEvent;
import com.homeverse.chat.dto.request.ChatMessageDTO;
import com.homeverse.chat.dto.response.ChatMessageResponse;
import com.homeverse.chat.dto.response.ConversationResponse;
import com.homeverse.chat.entity.Conversation;
import com.homeverse.chat.entity.Message;
import com.homeverse.chat.mapper.ChatMapper;
import com.homeverse.chat.repository.ConversationRepository;
import com.homeverse.chat.repository.MessageRepository;
import com.homeverse.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ChatMapper chatMapper;
    private final UserServiceClient userServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final ChatPresenceServiceImpl chatPresenceService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof Jwt)) {
            throw new RuntimeException("Token không hợp lệ hoặc không tìm thấy thông tin người dùng!");
        }

        Jwt jwt = (Jwt) auth.getPrincipal();
        return jwt.getClaim("userId");
    }

    @Override
    public ChatMessageResponse saveMessage(ChatMessageDTO dto) {
        Long currentUserId = getCurrentUserId();

        Conversation conversation = conversationRepository.findByUser1IdAndUser2IdOrUser1IdAndUser2Id(
                currentUserId, dto.getReceiverId(), dto.getReceiverId(), currentUserId)
                .orElseGet(() -> {
                    Conversation newConv = Conversation.builder()
                            .user1Id(currentUserId)
                            .user2Id(dto.getReceiverId())
                            .lastMessage(dto.getContent())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return conversationRepository.save(newConv);
                });

        conversation.setLastMessage(dto.getContent());
        conversation.setLastMessageSenderId(currentUserId);
        conversation.setUpdatedAt(LocalDateTime.now());

        if (conversation.getUser1Id().equals(dto.getReceiverId())) {
            conversation.setUser1Unread(
                    conversation.getUser1Unread() == null ? 1 : conversation.getUser1Unread() + 1);
        } else {
            conversation.setUser2Unread(
                    conversation.getUser2Unread() == null ? 1 : conversation.getUser2Unread() + 1);
        }

        conversationRepository.save(conversation);

        String replyToMessageId = dto.getReplyToMessageId();
        String replyPreview = null;
        Long replySenderId = null;

        if (replyToMessageId != null && !replyToMessageId.isBlank()) {
            Message repliedMessage = messageRepository.findById(replyToMessageId)
                    .orElse(null);

            if (repliedMessage != null) {
                replyPreview = repliedMessage.isRecalled()
                        ? "Tin nhắn đã được thu hồi"
                        : repliedMessage.getContent();

                replySenderId = repliedMessage.getSenderId();
            }
        }

        Message message = Message.builder()
                .conversationId(conversation.getId())
                .senderId(currentUserId)
                .receiverId(dto.getReceiverId())
                .content(dto.getContent())
                .type(dto.getType())
                .replyToMessageId(replyToMessageId)
                .mediaUrl(dto.getMediaUrl())
                .replyPreview(replyPreview)
                .replySenderId(replySenderId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Message savedMessage = messageRepository.save(message);
        ChatMessageResponse response = chatMapper.toResponse(savedMessage);

        try {
            messagingTemplate.convertAndSendToUser(
                    dto.getReceiverId().toString(),
                    "/queue/messages",
                    response);

            messagingTemplate.convertAndSendToUser(
                    currentUserId.toString(),
                    "/queue/messages",
                    response);
        } catch (Exception e) {
            log.error("WebSocket error: ", e);
        }

        try {
            NotificationEvent event = NotificationEvent.builder()
                    .receiverId(dto.getReceiverId())
                    .title("Tin nhắn mới")
                    .content(dto.getContent())
                    .type("CHAT_NEW")
                    .referenceId(currentUserId)
                    .build();
            kafkaTemplate.send("notification-topic", event);
        } catch (Exception e) {
            log.error("Kafka error: ", e);
        }

        return response;
    }

    @Override
    public List<ChatMessageResponse> getChatHistory(Long partnerId) {
        Long currentUserId = getCurrentUserId();
        return conversationRepository.findByUser1IdAndUser2IdOrUser1IdAndUser2Id(
                currentUserId, partnerId, partnerId, currentUserId)
                .map(conv -> messageRepository.findByConversationIdOrderByCreatedAtAsc(conv.getId())
                        .stream()
                        .map(chatMapper::toResponse)
                        .collect(Collectors.toList()))
                .orElse(new ArrayList<>());
    }

    @Override
    public List<ConversationResponse> getUserConversations() {
        Long currentUserId = getCurrentUserId();
        List<Conversation> conversations = conversationRepository
                .findByUser1IdOrUser2IdOrderByUpdatedAtDesc(currentUserId, currentUserId);

        return conversations.stream().map(c -> {
            Long partnerId = c.getUser1Id().equals(currentUserId) ? c.getUser2Id() : c.getUser1Id();
            int unreadCount = c.getUser1Id().equals(currentUserId)
                    ? (c.getUser1Unread() == null ? 0 : c.getUser1Unread())
                    : (c.getUser2Unread() == null ? 0 : c.getUser2Unread());

            String partnerName = "User " + partnerId;
            String partnerAvatar = null;
            try {
                var summary = userServiceClient.getUserSummary(partnerId);
                partnerName = summary.getFullName();
                partnerAvatar = summary.getAvatarUrl();
            } catch (Exception e) {
                log.warn("Feign call failed for user {}", partnerId);
            }

            return ConversationResponse.builder()
                    .conversationId(c.getId())
                    .partnerId(partnerId)
                    .fullName(partnerName)
                    .avatar(partnerAvatar)
                    .lastMessage(c.getLastMessage())
                    .lastTime(c.getUpdatedAt())
                    .unreadCount(unreadCount)
                    .online(chatPresenceService.isOnline(partnerId))
                    .lastSeen(chatPresenceService.getLastSeen(partnerId))
                    .build();

        }).collect(Collectors.toList());
    }

    @Override
    public void createConversationIfNotExists(Long partnerId) {
        Long currentUserId = getCurrentUserId();
        conversationRepository.findByUser1IdAndUser2IdOrUser1IdAndUser2Id(
                currentUserId, partnerId, partnerId, currentUserId)
                .orElseGet(() -> conversationRepository.save(Conversation.builder()
                        .user1Id(currentUserId)
                        .user2Id(partnerId)
                        .lastMessage("Bắt đầu trò chuyện")
                        .lastMessageSenderId(currentUserId)
                        .user1Unread(0)
                        .user2Unread(0)
                        .updatedAt(LocalDateTime.now())
                        .build()));
    }

    @Override
    public void markAsRead(Long partnerId) {
        Long currentUserId = getCurrentUserId();

        conversationRepository
                .findByUser1IdAndUser2IdOrUser1IdAndUser2Id(
                        currentUserId,
                        partnerId,
                        partnerId,
                        currentUserId)
                .ifPresent(conv -> {

                    LocalDateTime now = LocalDateTime.now();

                    List<Message> unread = messageRepository
                            .findByConversationIdAndReceiverIdAndIsReadFalse(
                                    conv.getId(),
                                    currentUserId);

                    unread.forEach(m -> {
                        m.setRead(true);
                        m.setReadAt(now);
                    });

                    messageRepository.saveAll(unread);

                    if (conv.getUser1Id().equals(currentUserId)) {
                        conv.setUser1Unread(0);
                        conv.setUser1LastReadAt(now);
                    } else {
                        conv.setUser2Unread(0);
                        conv.setUser2LastReadAt(now);
                    }

                    conversationRepository.save(conv);

                    messagingTemplate.convertAndSendToUser(
                            partnerId.toString(),
                            "/queue/read-receipts",
                            java.util.Map.of(
                                    "conversationId", conv.getId(),
                                    "readBy", currentUserId,
                                    "readAt", now.toString()));
                });
    }

    @Override
    public void recallMessage(String messageId) {
        Long currentUserId = getCurrentUserId();

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin nhắn"));

        if (!message.getSenderId().equals(currentUserId)) {
            throw new RuntimeException("Bạn chỉ có thể thu hồi tin nhắn của chính mình");
        }

        message.setContent("");
        message.setRecalled(true);
        message.setRecalledAt(LocalDateTime.now());

        Message saved = messageRepository.save(message);

        ChatMessageResponse response = chatMapper.toResponse(saved);

        messagingTemplate.convertAndSendToUser(
                message.getReceiverId().toString(),
                "/queue/message-recalled",
                response);

        messagingTemplate.convertAndSendToUser(
                message.getSenderId().toString(),
                "/queue/message-recalled",
                response);
    }
    @Override
public ChatMessageResponse reactMessage(String messageId, String emoji) {
    Long currentUserId = getCurrentUserId();

    Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy tin nhắn"));

    if (!message.getSenderId().equals(currentUserId)
            && !message.getReceiverId().equals(currentUserId)) {
        throw new RuntimeException("Bạn không có quyền reaction tin nhắn này");
    }

    if (message.getReactions() == null) {
        message.setReactions(new java.util.HashMap<>());
    }

    if (emoji == null || emoji.isBlank()) {
        message.getReactions().remove(currentUserId);
    } else {
        message.getReactions().put(currentUserId, emoji);
    }

    Message saved = messageRepository.save(message);
    ChatMessageResponse response = chatMapper.toResponse(saved);

    messagingTemplate.convertAndSendToUser(
            message.getReceiverId().toString(),
            "/queue/message-reaction",
            response
    );

    messagingTemplate.convertAndSendToUser(
            message.getSenderId().toString(),
            "/queue/message-reaction",
            response
    );

    return response;
}
}