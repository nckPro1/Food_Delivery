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
public class ShippingFeeSettingsDTO {
    
    private Long id;
    private BigDecimal fixedShippingFee;        // Phí ship cố định
    private BigDecimal freeShippingThreshold;   // Ngưỡng miễn phí ship
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

