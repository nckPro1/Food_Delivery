package com.example.food.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreSettingsDTO {
    
    private Long settingId;
    private String storeName;
    private String storePhone;
    private String storeEmail;
    private String storeCity;
    private String storeDistrict;
    private String storeWard;
    private String storeStreet;
    private String storeDescription;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
