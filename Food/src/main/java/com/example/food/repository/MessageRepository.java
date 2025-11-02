package com.example.food.repository;

import com.example.food.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId " +
           "AND m.createdAt > :since ORDER BY m.createdAt ASC")
    List<Message> findNewMessages(@Param("conversationId") Long conversationId, 
                                   @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversationId = :conversationId " +
           "AND m.isRead = false AND m.senderUserId != :userId")
    Long countUnreadMessages(@Param("conversationId") Long conversationId, 
                             @Param("userId") Long userId);

    @Query("SELECT m FROM Message m WHERE m.isRead = false AND m.senderUserId != :userId " +
           "AND m.conversationId IN (SELECT cp.conversationId FROM ConversationParticipant cp WHERE cp.userId = :userId)")
    List<Message> findUnreadMessagesForUser(@Param("userId") Long userId);
}

