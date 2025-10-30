package com.example.food.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodDTO {
    private String methodId;
    private String methodName;
    private String description;
    private String iconUrl;
    private boolean isActive;
    private boolean isOnline;
    private String paymentUrl;
}











