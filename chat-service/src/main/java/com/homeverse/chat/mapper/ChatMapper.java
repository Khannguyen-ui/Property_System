package com.homeverse.chat.mapper;

import com.homeverse.chat.dto.request.ChatMessageDTO;
import com.homeverse.chat.dto.response.ChatMessageResponse;
import com.homeverse.chat.entity.Message;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ChatMapper {
    
    @Autowired
    private ModelMapper modelMapper;

    public Message toEntity(ChatMessageDTO dto) {
        if (dto == null) return null;
        Message message = modelMapper.map(dto, Message.class);
        // Gán cứng ID để chắc chắn không bị trôi dữ liệu
        message.setSenderId(dto.getSenderId());
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

        // Kiểm tra an toàn cho Conversation
        if (entity.getConversation() != null) {
            Long u1 = entity.getConversation().getUser1Id();
            Long u2 = entity.getConversation().getUser2Id();
            Long senderId = entity.getSenderId();

            // Nếu người gửi là u1 thì người nhận là u2 và ngược lại
            if (senderId != null && u1 != null && u2 != null) {
                dto.setReceiverId(senderId.equals(u1) ? u2 : u1);
            }
        }

        return dto;
    }
}