package com.example.food.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    private Long userId;
    private String deliveryAddress; // Delivery address for this specific order
    private String deliveryNotes; // Optional delivery notes
    private String paymentMethod;
    private List<OrderItemRequest> orderItems;
    private String couponCode; // Optional coupon code

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        private Long productId;
        private Integer quantity;
        private String specialInstructions; // Optional special instructions
        private List<Long> selectedOptionIds; // IDs of selected product options
    }
}
