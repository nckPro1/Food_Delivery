package com.example.food.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ReviewDTO {
    private Long reviewId;
    private Long productId;
    private Long userId;
    private Long orderItemId;
    private Integer rating;
    private String comment;
    private List<String> imageUrls;
    private Integer imageCount;
    private Boolean isVerifiedPurchase;
    private Integer helpfulCount;
    private Boolean isVisible;
    private String adminReply;
    private LocalDateTime adminRepliedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


