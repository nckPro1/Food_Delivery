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
public class ShippingCalculationRequest {
    private BigDecimal orderAmount;
    private String deliveryCity;
    private String deliveryDistrict;
    private String deliveryWard;
    private String deliveryStreet;
}