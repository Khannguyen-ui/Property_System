package com.homeverse.chat.repository;

import com.homeverse.chat.entity.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {

    // 1. Tìm cuộc hội thoại giữa 2 người
    // MongoDB sẽ tự hiểu logic OR giữa các cặp user1Id và user2Id
    Optional<Conversation> findByUser1IdAndUser2IdOrUser1IdAndUser2Id(Long u1, Long u2, Long u2Again, Long u1Again);

    // 2. Lấy danh sách inbox của 1 người, sắp xếp theo thời gian mới nhất
    List<Conversation> findByUser1IdOrUser2IdOrderByUpdatedAtDesc(Long user1Id, Long user2Id);
}