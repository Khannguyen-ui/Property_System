package com.homeverse.chat.repository; // Nhớ đổi package cho đúng project mới

import com.homeverse.chat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // 1. Tìm cuộc hội thoại giữa 2 người (Dùng ID trần)
    // SỬA: c.user1.id -> c.user1Id
    @Query("SELECT c FROM Conversation c WHERE (c.user1Id = :u1 AND c.user2Id = :u2) " +
           "OR (c.user1Id = :u2 AND c.user2Id = :u1)")
    Optional<Conversation> findExistingConversation(@Param("u1") Long u1, @Param("u2") Long u2);

    // 2. Lấy danh sách inbox của 1 người
    // SỬA: c.user1.id -> c.user1Id
    @Query("SELECT c FROM Conversation c WHERE c.user1Id = :userId OR c.user2Id = :userId " +
           "ORDER BY c.updatedAt DESC")
    List<Conversation> findMyConversations(@Param("userId") Long userId);
}