package com.example.food.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
    private Long commentId;
    private Long productId;
    private Long userId;
    private Long reviewId; // optional link to a review
    private String content;
    private String userName;
    private String userAvatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

