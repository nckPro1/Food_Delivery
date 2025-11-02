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
public class SaleDTO {
    private Long saleId;
    private Long productId;
    private String productName;
    private String saleName;
    private String saleDescription;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal salePrice;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
