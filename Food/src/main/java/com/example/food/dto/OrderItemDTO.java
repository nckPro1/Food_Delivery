package com.example.food.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {

    private Long orderItemId;
    private Long orderId;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String specialInstructions;
    private List<OrderItemOptionDTO> orderItemOptions;
    private LocalDateTime createdAt;

    // Helper methods
    public BigDecimal getTotalPriceWithOptions() {
        BigDecimal basePrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal optionsPrice = BigDecimal.ZERO;

        if (orderItemOptions != null) {
            optionsPrice = orderItemOptions.stream()
                    .map(OrderItemOptionDTO::getExtraPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return basePrice.add(optionsPrice);
    }
}
