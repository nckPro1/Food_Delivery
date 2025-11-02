package com.example.food.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDTO {
    private Long conversationId;
    private String subject;
    private Long createdByUserId;
    private String createdByName;
    private String createdByAvatarUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long unreadCount;
    private MessageDTO lastMessage;
    private List<ParticipantDTO> participants;
}

