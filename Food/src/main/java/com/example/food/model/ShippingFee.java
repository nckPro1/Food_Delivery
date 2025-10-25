package com.example.food.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipping_fees")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingFee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipping_fee_id")
    private Long shippingFeeId;

    @Column(name = "fee_name", nullable = false)
    private String feeName;

    @Column(name = "fee_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "min_order_amount", precision = 10, scale = 2)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "max_order_amount", precision = 10, scale = 2)
    private BigDecimal maxOrderAmount;

    @Column(name = "free_shipping_threshold", precision = 10, scale = 2)
    private BigDecimal freeShippingThreshold = BigDecimal.valueOf(200000.00);

    @Column(name = "fee_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private FeeType feeType = FeeType.ORDER_BASED;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean isApplicableForOrder(BigDecimal orderAmount) {
        if (!isActive) return false;
        
        if (minOrderAmount != null && orderAmount.compareTo(minOrderAmount) < 0) {
            return false;
        }
        
        if (maxOrderAmount != null && orderAmount.compareTo(maxOrderAmount) > 0) {
            return false;
        }
        
        return true;
    }

    public BigDecimal calculateShippingFee(BigDecimal orderAmount) {
        if (!isApplicableForOrder(orderAmount)) {
            return BigDecimal.ZERO;
        }
        
        // If order amount >= free shipping threshold, return 0
        if (freeShippingThreshold != null && orderAmount.compareTo(freeShippingThreshold) >= 0) {
            return BigDecimal.ZERO;
        }
        
        return feeAmount;
    }
}