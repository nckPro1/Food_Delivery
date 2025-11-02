package com.example.food.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateOrUpdateReviewRequest {
    private Integer rating; // required 1..5
    private String comment; // optional short comment
    private List<String> imageUrls; // optional
}


