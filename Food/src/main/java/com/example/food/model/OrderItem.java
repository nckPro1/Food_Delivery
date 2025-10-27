package com.example.food.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Order_Items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "sale_price", precision = 10, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItemOption> orderItemOptions;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Helper methods
    public void calculateTotalPrice() {
        // Use sale price if available, otherwise use unit price
        BigDecimal priceToUse = (salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0)
                ? salePrice
                : unitPrice;

        BigDecimal basePrice = priceToUse.multiply(BigDecimal.valueOf(quantity));
        BigDecimal optionsPrice = BigDecimal.ZERO;

        if (orderItemOptions != null) {
            optionsPrice = orderItemOptions.stream()
                    .map(OrderItemOption::getExtraPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        this.totalPrice = basePrice.add(optionsPrice);
    }

    public BigDecimal getTotalPriceWithOptions() {
        calculateTotalPrice();
        return totalPrice;
    }
}
