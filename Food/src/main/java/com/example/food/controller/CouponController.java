package com.example.food.controller;

import com.example.food.dto.CouponDTO;
import com.example.food.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/validate")
    public ResponseEntity<CouponDTO> validateCoupon(@RequestParam String couponCode, @RequestParam BigDecimal orderAmount) {
        CouponDTO coupon = couponService.validateCoupon(couponCode, orderAmount);
        return ResponseEntity.ok(coupon);
    }

    @GetMapping("/active")
    public ResponseEntity<java.util.List<CouponDTO>> getActiveCoupons() {
        java.util.List<CouponDTO> coupons = couponService.getActiveCoupons();
        return ResponseEntity.ok(coupons);
    }
}
