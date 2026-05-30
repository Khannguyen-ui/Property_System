package com.homeverse.chat.mapper;

import com.homeverse.chat.dto.request.ChatMessageDTO;
import com.homeverse.chat.dto.response.ChatMessageResponse;
import com.homeverse.chat.entity.Conversation;
import com.homeverse.chat.entity.Message;
import com.homeverse.chat.repository.ConversationRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ChatMapper {
    
    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ConversationRepository conversationRepository;

    public Message toEntity(ChatMessageDTO dto) {
        if (dto == null) return null;
        Message message = modelMapper.map(dto, Message.class);
        message.setSenderId(dto.getSenderId());
        message.setConversationId(dto.getConversationId());
        return message;
    }

    public ChatMessageResponse toResponse(Message entity) {
        if (entity == null) return null;

        ChatMessageResponse dto = new ChatMessageResponse();
        dto.setId(entity.getId());
        dto.setContent(entity.getContent());
        dto.setType(entity.getType());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setSenderId(entity.getSenderId());

        if (entity.getConversationId() != null) {
            conversationRepository.findById(entity.getConversationId()).ifPresent(conv -> {
                Long u1 = conv.getUser1Id();
                Long u2 = conv.getUser2Id();
                Long senderId = entity.getSenderId();

                if (senderId != null && u1 != null && u2 != null) {
                    dto.setReceiverId(senderId.equals(u1) ? u2 : u1);
                }
            });
        }

        return dto;
    }
}