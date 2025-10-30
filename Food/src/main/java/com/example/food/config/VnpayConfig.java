package com.example.food.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class VnpayConfig {

    // VNPay Configuration
    @Value("${vnpay.tmn-code:0HW0BKYF}")
    private String vnpTmnCode;

    @Value("${vnpay.hash-secret:KVPHMNWHFD8Z6OT7FXFH28KK981HOPXZ}")
    private String vnpHashSecret;

    @Value("${vnpay.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpUrl;

    @Value("${vnpay.return-url:http://localhost:8080/api/payment/payment-callback}")
    private String vnpReturnUrl;

    // VNPay API version
    public static final String VNP_VERSION = "2.1.0";
    public static final String VNP_COMMAND = "pay";
    public static final String VNP_ORDER_TYPE = "other";
    public static final String VNP_CURRENCY_CODE = "VND";
    public static final String VNP_LOCALE = "vn";
}