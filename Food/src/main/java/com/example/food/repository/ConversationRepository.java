package com.example.food.repository;

import com.example.food.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // Tìm conversation của user (user là participant)
    @Query("SELECT DISTINCT c FROM Conversation c " +
           "JOIN ConversationParticipant cp ON c.conversationId = cp.conversationId " +
           "WHERE cp.userId = :userId " +
           "ORDER BY c.updatedAt DESC")
    List<Conversation> findByUserId(@Param("userId") Long userId);

    // Tìm conversation được tạo bởi user
    List<Conversation> findByCreatedByUserIdOrderByUpdatedAtDesc(Long userId);

    // Kiểm tra user có trong conversation không
    @Query("SELECT COUNT(cp) > 0 FROM ConversationParticipant cp " +
           "WHERE cp.conversationId = :conversationId AND cp.userId = :userId")
    boolean existsByConversationIdAndUserId(@Param("conversationId") Long conversationId, 
                                            @Param("userId") Long userId);

    // Tìm conversation của user và admin (để admin có thể tham gia cùng conversation với user)
    @Query("SELECT DISTINCT c FROM Conversation c " +
           "JOIN ConversationParticipant cp ON c.conversationId = cp.conversationId " +
           "WHERE cp.userId = :userId AND c.createdByUserId = :userId " +
           "ORDER BY c.updatedAt DESC")
    List<Conversation> findUserConversationsForAdmin(@Param("userId") Long userId);
}

