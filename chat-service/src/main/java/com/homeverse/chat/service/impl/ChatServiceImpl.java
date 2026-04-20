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
import org.springframework.security.oauth2.jwt.Jwt; // Import đúng chỗ này
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // Tách hàm này ra riêng biệt, không lồng trong hàm khác
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !(auth.getPrincipal() instanceof Jwt)) {
            throw new RuntimeException("Token không hợp lệ hoặc không tìm thấy thông tin người dùng!");
        }

        Jwt jwt = (Jwt) auth.getPrincipal();
        // Lấy userId từ extraClaims mà Identity Service đã đóng gói
        return jwt.getClaim("userId"); 
    }

    @Override
    @Transactional
    public ChatMessageResponse saveMessage(ChatMessageDTO dto) {
        // Lấy senderId từ Token thay vì tin tưởng hoàn toàn vào DTO để bảo mật
        Long currentUserId = getCurrentUserId();

        Conversation conversation = conversationRepository.findExistingConversation(currentUserId, dto.getReceiverId())
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
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        Message message = Message.builder()
                .conversation(conversation)
                .senderId(currentUserId)
                .content(dto.getContent())
                .type(dto.getType())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        Message savedMessage = messageRepository.save(message);

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

        return chatMapper.toResponse(savedMessage);
    }

    @Override
    public List<ChatMessageResponse> getChatHistory(Long partnerId) {
        Long currentUserId = getCurrentUserId();
        return conversationRepository.findExistingConversation(currentUserId, partnerId)
                .map(conv -> messageRepository.findByConversationIdOrderByCreatedAtAsc(conv.getId())
                        .stream()
                        .map(chatMapper::toResponse)
                        .collect(Collectors.toList()))
                .orElse(new ArrayList<>());
    }

    @Override
    public List<ConversationResponse> getUserConversations() {
        Long currentUserId = getCurrentUserId();
        List<Conversation> conversations = conversationRepository.findMyConversations(currentUserId);

        return conversations.stream().map(c -> {
            Long partnerId = c.getUser1Id().equals(currentUserId) ? c.getUser2Id() : c.getUser1Id();
            long unreadCount = messageRepository.countUnreadMessages(c.getId(), currentUserId);

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
                    .id(partnerId)
                    .fullName(partnerName)
                    .avatar(partnerAvatar)
                    .lastMessage(c.getLastMessage())
                    .lastTime(c.getUpdatedAt())
                    .unreadCount((int) unreadCount)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void createConversationIfNotExists(Long partnerId) {
        Long currentUserId = getCurrentUserId();
        conversationRepository.findExistingConversation(currentUserId, partnerId)
                .orElseGet(() -> conversationRepository.save(Conversation.builder()
                        .user1Id(currentUserId)
                        .user2Id(partnerId)
                        .lastMessage("Bắt đầu trò chuyện")
                        .updatedAt(LocalDateTime.now())
                        .build()));
    }

    @Override
    @Transactional
    public void markAsRead(Long partnerId) {
        Long currentUserId = getCurrentUserId();
        conversationRepository.findExistingConversation(currentUserId, partnerId)
                .ifPresent(conv -> {
                    List<Message> unread = messageRepository.findByConversationIdAndSenderIdNotAndIsReadFalse(conv.getId(), currentUserId);
                    unread.forEach(m -> m.setRead(true));
                    messageRepository.saveAll(unread);
                });
    }
}