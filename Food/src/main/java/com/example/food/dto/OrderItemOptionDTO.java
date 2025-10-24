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
public class OrderItemOptionDTO {

    private Long orderItemOptionId;
    private Long orderItemId;
    private Long optionId;
    private String optionName;
    private String optionType;
    private BigDecimal extraPrice;
}
