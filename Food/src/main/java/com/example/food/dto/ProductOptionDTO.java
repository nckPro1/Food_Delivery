package com.example.food.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOptionDTO {
    private Long optionId;
    private Long productId;
    private String optionName;
    private String optionType;
    private BigDecimal price;  // Changed from extraPrice to price
    private Boolean isRequired;
    private Boolean isActive;  // Added missing field
    private Integer maxSelections;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
