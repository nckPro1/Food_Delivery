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
public class ShippingFeeDTO {
    
    private Long shippingFeeId;
    private String feeName;
    private BigDecimal feeAmount;
    private BigDecimal minOrderAmount;
    private BigDecimal maxOrderAmount;
    private BigDecimal freeShippingThreshold;
    private BigDecimal perKmFee;
    private Boolean isDefault;
    private Boolean isActive;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

