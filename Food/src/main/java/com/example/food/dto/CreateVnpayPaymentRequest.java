package com.example.food.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVnpayPaymentRequest {
    private Long orderId;
    private Long amount; // Số tiền (VND)
    private String orderInfo;
    private String ipAddr;
}