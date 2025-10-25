package com.example.food.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingFeeSettingsDTO {
    
    private Long id;
    private Integer sameDistrictFee;
    private Integer differentDistrictFee;
    private Integer outsideCityFee;
    private Integer freeShippingThreshold;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

