package com.example.food.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewActivityDTO {
    private String type; // REVIEW or COMMENT
    private Long id; // reviewId or commentId
    private Long productId;
    private String productName;
    private Long userId;
    private String userName;
    private Integer rating; // for REVIEW
    private String content; // for COMMENT or short review comment
    private LocalDateTime createdAt;
}


