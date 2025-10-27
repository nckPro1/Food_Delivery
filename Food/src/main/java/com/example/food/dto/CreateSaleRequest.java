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
public class CreateSaleRequest {
    private Long saleId; // For update operations
    private Long productId;
    private String saleName;
    private String saleDescription;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal salePrice;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
