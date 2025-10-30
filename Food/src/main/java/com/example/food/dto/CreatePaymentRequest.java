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
public class CreatePaymentRequest {
    private Long orderId;
    private String paymentMethod;
    private BigDecimal amount;
    private String description;
    private String returnUrl;
    private String cancelUrl;
    private String ipAddress;
    private String bankCode;
}











