package com.example.food.controller;

import com.example.food.dto.*;
import com.example.food.service.OrderService;
import com.example.food.service.EnhancedShippingFeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final OrderService orderService;
    private final EnhancedShippingFeeService enhancedShippingFeeService;

    /**
     * Test endpoint để kiểm tra luồng order với COD
     */
    @PostMapping("/order-cod-flow")
    public ResponseEntity<ApiResponse<String>> testOrderCodFlow() {
        try {
            log.info("Testing COD order flow...");

            // 1. Test tính phí ship với khoảng cách
            ShippingCalculationRequest shippingRequest = ShippingCalculationRequest.builder()
                .orderAmount(BigDecimal.valueOf(100000))
                .deliveryLatitude(BigDecimal.valueOf(10.7769)) // Quận 1, TP.HCM
                .deliveryLongitude(BigDecimal.valueOf(106.7009))
                .build();

            ShippingCalculationResponse shippingResponse = enhancedShippingFeeService
                .calculateShippingFee(shippingRequest.getOrderAmount(), 
                                    shippingRequest.getDeliveryLatitude(), 
                                    shippingRequest.getDeliveryLongitude());

            log.info("Shipping calculation result: {} VND, Distance: {} km", 
                    shippingResponse.getShippingFee(), shippingResponse.getDistanceKm());

            // 2. Test tạo đơn hàng với COD
            CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .userId(2L) // Test user
                .deliveryAddress("123 Đường ABC, Quận 1, TP.HCM")
                .deliveryLatitude(BigDecimal.valueOf(10.7769))
                .deliveryLongitude(BigDecimal.valueOf(106.7009))
                .paymentMethod("CASH")
                .orderItems(Arrays.asList(
                    CreateOrderRequest.OrderItemRequest.builder()
                        .productId(1L)
                        .quantity(2)
                        .build()
                ))
                .build();

            OrderDTO order = orderService.createOrder(orderRequest);

            log.info("Order created successfully: {}", order.getOrderNumber());

            return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("COD order flow test completed successfully")
                .data("Order Number: " + order.getOrderNumber() + 
                      ", Shipping Fee: " + order.getShippingFee() + 
                      ", Distance: " + order.getCalculatedDistanceKm() + " km")
                .build());

        } catch (Exception e) {
            log.error("Error in COD order flow test: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                .success(false)
                .message("COD order flow test failed: " + e.getMessage())
                .build());
        }
    }

    /**
     * Test endpoint để kiểm tra tính phí ship
     */
    @PostMapping("/shipping-calculation")
    public ResponseEntity<ApiResponse<ShippingCalculationResponse>> testShippingCalculation(
            @RequestParam(defaultValue = "100000") BigDecimal orderAmount,
            @RequestParam(defaultValue = "10.7769") BigDecimal lat,
            @RequestParam(defaultValue = "106.7009") BigDecimal lng) {
        try {
            ShippingCalculationResponse response = enhancedShippingFeeService
                .calculateShippingFee(orderAmount, lat, lng);

            return ResponseEntity.ok(ApiResponse.<ShippingCalculationResponse>builder()
                .success(true)
                .message("Shipping calculation test completed")
                .data(response)
                .build());

        } catch (Exception e) {
            log.error("Error in shipping calculation test: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<ShippingCalculationResponse>builder()
                .success(false)
                .message("Shipping calculation test failed: " + e.getMessage())
                .build());
        }
    }
}
