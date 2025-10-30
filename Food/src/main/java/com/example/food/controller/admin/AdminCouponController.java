package com.example.food.controller.admin;

import com.example.food.dto.CreateCouponRequest;
import com.example.food.dto.CouponDTO;
import com.example.food.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
@Slf4j
public class AdminCouponController {

    private final CouponService couponService;

    /**
     * Trang danh sách coupons
     */
    @GetMapping
    public String couponsList(Model model) {
        log.info("Loading coupons list page");
        
        List<CouponDTO> coupons = couponService.getAllCoupons();
        model.addAttribute("coupons", coupons);
        model.addAttribute("pageTitle", "Coupon - Danh sách");
        
        return "admin/coupons/list";
    }

    /**
     * Trang thêm coupon mới
     */
    @GetMapping("/add")
    public String addCouponForm(Model model) {
        log.info("Loading add coupon form");
        
        model.addAttribute("coupon", new CreateCouponRequest());
        model.addAttribute("pageTitle", "Coupon - Thêm mới");
        
        return "admin/coupons/form";
    }

    /**
     * Trang sửa coupon
     */
    @GetMapping("/edit/{id}")
    public String editCouponForm(@PathVariable Long id, Model model) {
        log.info("Loading edit coupon form for ID: {}", id);
        
        CouponDTO couponDTO = couponService.getCouponById(id);
        
        // Convert CouponDTO to CreateCouponRequest for editing
        CreateCouponRequest coupon = CreateCouponRequest.builder()
                .couponId(couponDTO.getCouponId())
                .couponCode(couponDTO.getCouponCode())
                .couponName(couponDTO.getCouponName())
                .description(couponDTO.getDescription())
                .discountType(couponDTO.getDiscountType())
                .discountValue(couponDTO.getDiscountValue())
                .minOrderAmount(couponDTO.getMinOrderAmount())
                .maxDiscountAmount(couponDTO.getMaxDiscountAmount())
                .usageLimit(couponDTO.getUsageLimit())
                .startDate(couponDTO.getStartDate())
                .endDate(couponDTO.getEndDate())
                .build();
        
        model.addAttribute("coupon", coupon);
        model.addAttribute("pageTitle", "Coupon - Chỉnh sửa");
        
        return "admin/coupons/form";
    }

    /**
     * Xử lý thêm/sửa coupon
     */
    @PostMapping("/save")
    public String saveCoupon(@ModelAttribute CreateCouponRequest request, RedirectAttributes redirectAttributes) {
        try {
            log.info("Saving coupon: {}", request.getCouponCode());
            
            // Check if this is an update (has couponId) or create (no couponId)
            if (request.getCouponId() != null && request.getCouponId() > 0) {
                couponService.updateCoupon(request.getCouponId(), request);
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật coupon thành công!");
            } else {
                couponService.createCoupon(request);
                redirectAttributes.addFlashAttribute("successMessage", "Thêm coupon thành công!");
            }
            
        } catch (Exception e) {
            log.error("Error saving coupon: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu coupon: " + e.getMessage());
        }
        
        return "redirect:/admin/coupons";
    }

    /**
     * Xóa coupon
     */
    @PostMapping("/delete/{id}")
    public String deleteCoupon(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            log.info("Deleting coupon with ID: {}", id);
            
            couponService.deleteCoupon(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa coupon thành công!");
            
        } catch (Exception e) {
            log.error("Error deleting coupon: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa coupon: " + e.getMessage());
        }
        
        return "redirect:/admin/coupons";
    }

    /**
     * Kích hoạt/vô hiệu hóa coupon
     */
    @PostMapping("/toggle/{id}")
    public String toggleCoupon(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            log.info("Toggling coupon with ID: {}", id);
            
            couponService.toggleCouponStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái coupon thành công!");
            
        } catch (Exception e) {
            log.error("Error toggling coupon: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật trạng thái coupon: " + e.getMessage());
        }
        
        return "redirect:/admin/coupons";
    }
}
