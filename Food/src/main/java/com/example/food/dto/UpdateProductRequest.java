package com.example.food.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private List<String> galleryUrls;
    private Boolean hasOptions;
    private Boolean isAvailable;
    private Boolean isFeatured;
    private Integer preparationTime;
    private Long categoryId;
}
