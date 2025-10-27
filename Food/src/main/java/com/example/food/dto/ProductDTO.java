package com.example.food.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long productId;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private List<String> galleryUrls;
    private Boolean hasOptions;
    private Boolean isAvailable;
    private Boolean isFeatured;
    private Integer preparationTime;
    private CategoryDTO category;
    private List<ProductOptionDTO> options; // Add options field

    // Sale fields
    private BigDecimal salePrice;
    private Integer salePercentage;
    private Boolean isOnSale;
    private String saleStartDate;
    private String saleEndDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
