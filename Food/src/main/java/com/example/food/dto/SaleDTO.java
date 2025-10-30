package com.example.food.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SaleDTO {
    private Long saleId;
    private Long productId;
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

    // Thông tin sản phẩm
    private String productName;
    private String productImageUrl;
    private BigDecimal originalPrice;
    private String productDescription;
    private String categoryName;
}