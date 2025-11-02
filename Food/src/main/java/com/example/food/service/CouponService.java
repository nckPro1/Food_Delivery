package com.example.food.service;

import com.example.food.dto.CreateCouponRequest;
import com.example.food.dto.CouponDTO;
import com.example.food.model.Coupon;
import com.example.food.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    public List<CouponDTO> getAllCoupons() {
        return couponRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CouponDTO> getActiveCoupons() {
        return couponRepository.findAll().stream()
                .filter(coupon -> coupon.isValid())
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public CouponDTO getCouponById(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));
        return convertToDTO(coupon);
    }

    @Transactional
    public CouponDTO createCoupon(CreateCouponRequest request) {
        // Check if coupon code already exists
        if (couponRepository.existsByCouponCode(request.getCouponCode())) {
            throw new RuntimeException("Coupon code already exists");
        }

        Coupon coupon = Coupon.builder()
                .couponCode(request.getCouponCode())
                .couponName(request.getCouponName())
                .description(request.getDescription())
                .discountType(Coupon.DiscountType.valueOf(request.getDiscountType()))
                .discountValue(request.getDiscountValue())
                .minOrderAmount(request.getMinOrderAmount())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .usageLimit(request.getUsageLimit())
                .usedCount(0)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(true)
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);
        return convertToDTO(savedCoupon);
    }

    @Transactional
    public CouponDTO updateCoupon(Long couponId, CreateCouponRequest request) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));

        // Check if coupon code already exists (excluding current coupon)
        if (!coupon.getCouponCode().equals(request.getCouponCode()) && 
            couponRepository.existsByCouponCode(request.getCouponCode())) {
            throw new RuntimeException("Coupon code already exists");
        }

        coupon.setCouponCode(request.getCouponCode());
        coupon.setCouponName(request.getCouponName());
        coupon.setDescription(request.getDescription());
        coupon.setDiscountType(Coupon.DiscountType.valueOf(request.getDiscountType()));
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderAmount(request.getMinOrderAmount());
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setStartDate(request.getStartDate());
        coupon.setEndDate(request.getEndDate());

        Coupon savedCoupon = couponRepository.save(coupon);
        return convertToDTO(savedCoupon);
    }

    @Transactional
    public void deleteCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));

        couponRepository.delete(coupon);
    }

    @Transactional
    public CouponDTO toggleCouponStatus(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));

        coupon.setIsActive(!coupon.getIsActive());
        Coupon savedCoupon = couponRepository.save(coupon);
        return convertToDTO(savedCoupon);
    }

    public CouponDTO validateCoupon(String couponCode, BigDecimal orderAmount) {
        Coupon coupon = couponRepository.findValidCoupon(couponCode, LocalDateTime.now())
                .orElse(null);
        if (coupon == null) {
            return CouponDTO.builder()
                    .canUse(false)
                    .message("Mã giảm giá không hợp lệ hoặc đã hết hạn.")
                    .build();
        }
        if (orderAmount == null || coupon.getMinOrderAmount() == null) {
            return CouponDTO.builder()
                    .canUse(false)
                    .message("Chưa truyền tổng tiền đơn hàng hợp lệ.")
                    .build();
        }
        if (orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            return CouponDTO.builder()
                    .couponId(coupon.getCouponId())
                    .couponCode(coupon.getCouponCode())
                    .couponName(coupon.getCouponName())
                    .description(coupon.getDescription())
                    .discountType(coupon.getDiscountType() != null ? coupon.getDiscountType().name() : null)
                    .discountValue(coupon.getDiscountValue())
                    .minOrderAmount(coupon.getMinOrderAmount())
                    .maxDiscountAmount(coupon.getMaxDiscountAmount())
                    .usageLimit(coupon.getUsageLimit())
                    .usedCount(coupon.getUsedCount())
                    .startDate(coupon.getStartDate())
                    .endDate(coupon.getEndDate())
                    .isActive(coupon.getIsActive())
                    .canUse(false)
                    .message("Đơn hàng chưa đủ điều kiện. Tổng tiền từ " + coupon.getMinOrderAmount() + " mới áp dụng được coupon này.")
                    .build();
        }
        if (!coupon.canBeUsedForOrder(orderAmount)) {
            return CouponDTO.builder()
                    .couponId(coupon.getCouponId())
                    .couponCode(coupon.getCouponCode())
                    .couponName(coupon.getCouponName())
                    .description(coupon.getDescription())
                    .discountType(coupon.getDiscountType() != null ? coupon.getDiscountType().name() : null)
                    .discountValue(coupon.getDiscountValue())
                    .minOrderAmount(coupon.getMinOrderAmount())
                    .maxDiscountAmount(coupon.getMaxDiscountAmount())
                    .usageLimit(coupon.getUsageLimit())
                    .usedCount(coupon.getUsedCount())
                    .startDate(coupon.getStartDate())
                    .endDate(coupon.getEndDate())
                    .isActive(coupon.getIsActive())
                    .canUse(false)
                    .message("Coupon không hợp lệ hoặc đã vượt quá số lượt dùng.")
                    .build();
        }
        CouponDTO dto = convertToDTO(coupon);
        dto.setCanUse(true);
        dto.setMessage("Áp dụng thành công!");
        return dto;
    }

    @Transactional
    public void incrementCouponUsage(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));

        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);
    }

    @Transactional
    public int deactivateExpiredCoupons() {
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> expired = couponRepository.findAll().stream()
            .filter(c -> c.getIsActive() && c.getEndDate() != null && now.isAfter(c.getEndDate()))
            .collect(java.util.stream.Collectors.toList());
        for (Coupon c : expired) {
            c.setIsActive(false);
        }
        couponRepository.saveAll(expired);
        return expired.size();
    }

    private CouponDTO convertToDTO(Coupon coupon) {
        return CouponDTO.builder()
                .couponId(coupon.getCouponId())
                .couponCode(coupon.getCouponCode())
                .couponName(coupon.getCouponName())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType().name())
                .discountValue(coupon.getDiscountValue())
                .minOrderAmount(coupon.getMinOrderAmount())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                .startDate(coupon.getStartDate())
                .endDate(coupon.getEndDate())
                .isActive(coupon.getIsActive())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }
}
