package com.homeverse.chat.service;

import com.homeverse.chat.dto.request.ChatMessageDTO;
import com.homeverse.chat.dto.response.ChatMessageResponse;
import com.homeverse.chat.dto.response.ConversationResponse;
import java.util.List;

public interface ChatService {

    /**
     * Lưu tin nhắn và trả về Response DTO để Controller phản hồi ngay hoặc bắn qua Socket.
     */
    ChatMessageResponse saveMessage(ChatMessageDTO dto);

    /**
     * Lấy lịch sử chat. PartnerId lấy từ URL, còn CurrentUserId sẽ lấy từ Token trong Implementation.
     */
    List<ChatMessageResponse> getChatHistory(Long partnerId);

    /**
     * Lấy danh sách sidebar. Service sẽ tự xác định CurrentUserId từ SecurityContext.
     */
    List<ConversationResponse> getUserConversations();

    /**
     * Tạo hội thoại trống (ví dụ khi nhấn "Nhắn tin" từ trang chi tiết sản phẩm/phòng).
     */
    void createConversationIfNotExists(Long partnerId);

    /**
     * Đánh dấu đã đọc dựa trên PartnerId.
     */
    void markAsRead(Long partnerId);
}