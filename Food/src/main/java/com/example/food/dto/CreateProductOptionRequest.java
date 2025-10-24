package com.example.food.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductOptionRequest {
    private String optionName;
    private String optionType;
    private BigDecimal price;  // Changed from extraPrice to price
    private Boolean isRequired;
    private Boolean isActive;  // Added missing field
    private Integer maxSelections;
}
