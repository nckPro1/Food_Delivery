package com.example.food.controller.admin;

import com.example.food.dto.CreateCouponRequest;
import com.example.food.dto.CouponDTO;
import com.example.food.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;

    @GetMapping
    public ResponseEntity<List<CouponDTO>> getAllCoupons() {
        List<CouponDTO> coupons = couponService.getAllCoupons();
        return ResponseEntity.ok(coupons);
    }

    @GetMapping("/active")
    public ResponseEntity<List<CouponDTO>> getActiveCoupons() {
        List<CouponDTO> coupons = couponService.getActiveCoupons();
        return ResponseEntity.ok(coupons);
    }

    @GetMapping("/{couponId}")
    public ResponseEntity<CouponDTO> getCouponById(@PathVariable Long couponId) {
        CouponDTO coupon = couponService.getCouponById(couponId);
        return ResponseEntity.ok(coupon);
    }

    @PostMapping
    public ResponseEntity<CouponDTO> createCoupon(@RequestBody CreateCouponRequest request) {
        CouponDTO coupon = couponService.createCoupon(request);
        return ResponseEntity.ok(coupon);
    }

    @PutMapping("/{couponId}")
    public ResponseEntity<CouponDTO> updateCoupon(@PathVariable Long couponId, @RequestBody CreateCouponRequest request) {
        CouponDTO coupon = couponService.updateCoupon(couponId, request);
        return ResponseEntity.ok(coupon);
    }

    @DeleteMapping("/{couponId}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long couponId) {
        couponService.deleteCoupon(couponId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{couponId}/toggle")
    public ResponseEntity<CouponDTO> toggleCouponStatus(@PathVariable Long couponId) {
        CouponDTO coupon = couponService.toggleCouponStatus(couponId);
        return ResponseEntity.ok(coupon);
    }
}
