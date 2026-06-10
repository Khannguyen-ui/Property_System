package com.homeverse.chat.service;

import com.homeverse.chat.dto.request.ChatMessageDTO;
import com.homeverse.chat.dto.response.ChatMessageResponse;
import com.homeverse.chat.dto.response.ConversationResponse;
import java.util.List;

public interface ChatService {

    ChatMessageResponse saveMessage(ChatMessageDTO dto);

    List<ChatMessageResponse> getChatHistory(Long partnerId);

    List<ConversationResponse> getUserConversations();

    void createConversationIfNotExists(Long partnerId);

    void markAsRead(Long partnerId);
    void recallMessage(String messageId);
    ChatMessageResponse reactMessage(String messageId, String emoji);
    
}