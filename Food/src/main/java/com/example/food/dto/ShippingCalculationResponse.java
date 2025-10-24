package com.example.food.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingCalculationResponse {
    private BigDecimal shippingFee;
    private BigDecimal distanceKm;
    private Integer estimatedDurationMinutes;
    private boolean fromCache;
    private String description;
}