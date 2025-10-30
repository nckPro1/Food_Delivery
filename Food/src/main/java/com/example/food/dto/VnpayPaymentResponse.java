package com.example.food.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VnpayPaymentResponse {
    private String paymentUrl;
    private String txnRef;
    private Long amount;
    private String orderInfo;
}