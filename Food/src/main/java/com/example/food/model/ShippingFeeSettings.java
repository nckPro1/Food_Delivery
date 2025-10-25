package com.example.food.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @Column(name = "same_district_fee", nullable = false)
    @Builder.Default
    private Integer sameDistrictFee = 10000;

    @Column(name = "different_district_fee", nullable = false)
    @Builder.Default
    private Integer differentDistrictFee = 30000;

    @Column(name = "outside_city_fee", nullable = false)
    @Builder.Default
    private Integer outsideCityFee = 0;

    @Column(name = "free_shipping_threshold", nullable = false)
    @Builder.Default
    private Integer freeShippingThreshold = 200000;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

