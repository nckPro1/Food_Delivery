package com.example.food.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateCommentRequest {
    private Long reviewId; // optional link to a review
    private String content; // required
    private List<String> attachmentUrls; // optional
}


