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
public class ShippingFeeInfo {
    private BigDecimal defaultShippingFee;
    private BigDecimal freeShippingThreshold;
    private BigDecimal minOrderAmount;
}
