package com.homeverse.chat.repository;

import com.homeverse.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // 1. Lấy lịch sử chat của 1 hội thoại (Giữ nguyên - Rất tốt)
    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    // 2. Đếm số tin nhắn chưa đọc
    // SỬA: m.sender.id -> m.senderId (vì Message entity giờ chỉ lưu Long senderId)
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId " +
            "AND m.isRead = false AND m.senderId != :userId")
    long countUnreadMessages(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    // 3. Tìm các tin nhắn chưa đọc để cập nhật trạng thái "Đã đọc"
    // SỬA: Tên hàm phải khớp với field senderId trong Entity mới
    List<Message> findByConversationIdAndSenderIdNotAndIsReadFalse(Long conversationId, Long userId);
}