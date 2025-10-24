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
@Table(name = "store_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Long settingId;

    @Column(name = "store_name", nullable = false)
    @Builder.Default
    private String storeName = "Nhà hàng của tôi";

    @Column(name = "store_phone")
    private String storePhone;

    @Column(name = "store_email")
    private String storeEmail;

    @Column(name = "store_city")
    private String storeCity;

    @Column(name = "store_district")
    private String storeDistrict;

    @Column(name = "store_ward")
    private String storeWard;

    @Column(name = "store_street")
    private String storeStreet;

    @Column(name = "store_description", columnDefinition = "TEXT")
    private String storeDescription;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
