package com.example.food.repository;

import com.example.food.model.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    List<ConversationParticipant> findByConversationId(Long conversationId);

    List<ConversationParticipant> findByUserId(Long userId);

    List<ConversationParticipant> findByConversationIdAndUserId(Long conversationId, Long userId);

    @Query("SELECT cp.userId FROM ConversationParticipant cp WHERE cp.conversationId = :conversationId")
    List<Long> findUserIdsByConversationId(@Param("conversationId") Long conversationId);
}

