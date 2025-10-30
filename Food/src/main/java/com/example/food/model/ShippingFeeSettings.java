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
@Table(name = "shipping_fee_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingFeeSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fixed_shipping_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal fixedShippingFee = BigDecimal.valueOf(15000); // 15,000 VNĐ

    @Column(name = "free_shipping_threshold", nullable = false, precision = 10, scale = 2)
    private BigDecimal freeShippingThreshold = BigDecimal.valueOf(200000); // 200,000 VNĐ

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Tính phí ship dựa trên tổng đơn hàng
     */
    public BigDecimal calculateShippingFee(BigDecimal orderAmount) {
        if (!enabled) {
            return BigDecimal.ZERO;
        }

        // Nếu đơn hàng >= ngưỡng miễn phí ship thì free
        if (orderAmount.compareTo(freeShippingThreshold) >= 0) {
            return BigDecimal.ZERO;
        }

        // Ngược lại tính phí ship cố định
        return fixedShippingFee;
    }
}
