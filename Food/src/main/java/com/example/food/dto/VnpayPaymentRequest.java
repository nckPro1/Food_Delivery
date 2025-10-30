package com.example.food.dto;

import lombok.Data;

@Data
public class VnpayPaymentRequest {
    private long amount;
    private String orderInfo;
    private String orderType = "other";
    private String language = "vn";
    private String bankCode = "";
}