package com.example.food.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceQuoteDTO {
    private BigDecimal subtotal;      // Tổng tiền hàng (bao gồm options)
    private BigDecimal shippingFee;   // Phí vận chuyển
    private BigDecimal couponDiscount; // Giảm giá coupon (nếu có)
    private BigDecimal finalAmount;   // Thành tiền cuối
}


