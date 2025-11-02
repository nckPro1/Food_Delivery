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
public class MessageDTO {
    private Long messageId;
    private Long conversationId;
    private Long senderUserId;
    private String senderName;
    private String senderAvatarUrl;
    private String content;
    private List<String> attachmentUrls;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private Boolean isOwnMessage; // true nếu là tin nhắn của user hiện tại
}

