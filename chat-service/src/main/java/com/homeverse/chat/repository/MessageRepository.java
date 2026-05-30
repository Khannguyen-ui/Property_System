package com.homeverse.chat.repository;

import com.homeverse.chat.entity.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    // 1. Lấy lịch sử chat của 1 hội thoại
    List<Message> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    // 2. Đếm số tin nhắn chưa đọc
    // MongoDB sẽ tự hiểu đếm các tin nhắn thuộc conversationId, chưa đọc và không phải của userId gửi
    long countByConversationIdAndIsReadFalseAndSenderIdNot(String conversationId, Long userId);

    // 3. Tìm các tin nhắn chưa đọc để cập nhật trạng thái "Đã đọc"
    List<Message> findByConversationIdAndSenderIdNotAndIsReadFalse(String conversationId, Long userId);
}