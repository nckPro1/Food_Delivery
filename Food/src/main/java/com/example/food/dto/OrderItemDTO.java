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
    private BigDecimal salePrice;
    private BigDecimal totalPrice;
    private String specialInstructions;
    private List<OrderItemOptionDTO> orderItemOptions;
    private LocalDateTime createdAt;

    // Helper methods
    public BigDecimal getTotalPriceWithOptions() {
        // Dùng salePrice nếu có, fallback unitPrice
        BigDecimal priceToUse = (salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0)
                ? salePrice
                : unitPrice;

        BigDecimal basePrice = priceToUse.multiply(BigDecimal.valueOf(quantity));
        BigDecimal optionsPrice = BigDecimal.ZERO;

        if (orderItemOptions != null) {
            BigDecimal singleItemOptions = orderItemOptions.stream()
                    .map(OrderItemOptionDTO::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            // Options áp dụng cho mỗi đơn vị sản phẩm
            optionsPrice = singleItemOptions.multiply(BigDecimal.valueOf(quantity));
        }

        return basePrice.add(optionsPrice);
    }
}
