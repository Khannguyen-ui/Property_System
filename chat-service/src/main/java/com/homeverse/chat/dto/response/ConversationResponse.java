package com.homeverse.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {
    
    // ID của cuộc hội thoại trong MongoDB (Để FE gọi API lấy tin nhắn)
    private String conversationId; 

    // ID của người đối diện (Partner ID)
    private Long partnerId; 

    // Thông tin hiển thị của người đối diện
    private String fullName;
    private String avatar;

    // Tin nhắn cuối cùng
    private String lastMessage;
    private LocalDateTime lastTime;

    // Trạng thái
    private boolean isOnline;
    private int unreadCount;
}