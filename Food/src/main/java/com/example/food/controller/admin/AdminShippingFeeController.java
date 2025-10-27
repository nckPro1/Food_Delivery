package com.example.food.controller.admin;

import com.example.food.dto.ShippingFeeSettingsDTO;
import com.example.food.service.ShippingFeeSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminShippingFeeController {

    private final ShippingFeeSettingsService shippingFeeSettingsService;

    /**
     * Trang danh sách phí ship
     */
    @GetMapping("/shipping-fees/list")
    public String shippingFeesList(Model model) {
        log.info("Loading shipping fees list page");
        
        // Lấy cài đặt phí ship hiện tại
        Optional<ShippingFeeSettingsDTO> shippingSettings = shippingFeeSettingsService.getCurrentSettings();
        model.addAttribute("shippingSettings", shippingSettings.orElse(new ShippingFeeSettingsDTO()));
        model.addAttribute("pageTitle", "Phí ship - Danh sách");
        
        return "admin/shipping-fees/list";
    }

    /**
     * Trang chỉnh sửa phí ship
     */
    @GetMapping("/shipping-fees/edit")
    public String editShippingFeesForm(Model model) {
        log.info("Loading edit shipping fees form");
        
        // Lấy cài đặt phí ship hiện tại
        Optional<ShippingFeeSettingsDTO> shippingSettings = shippingFeeSettingsService.getCurrentSettings();
        model.addAttribute("shippingSettings", shippingSettings.orElse(new ShippingFeeSettingsDTO()));
        model.addAttribute("pageTitle", "Phí ship - Chỉnh sửa");
        
        return "admin/shipping-fees/form";
    }

    /**
     * Cập nhật cài đặt phí ship
     */
    @PostMapping("/shipping-fees/update")
    public String updateShippingSettings(
            @RequestParam BigDecimal fixedShippingFee,
            @RequestParam BigDecimal freeShippingThreshold,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("Updating shipping settings: fixed={}, threshold={}", fixedShippingFee, freeShippingThreshold);

            ShippingFeeSettingsDTO settingsDTO = ShippingFeeSettingsDTO.builder()
                    .fixedShippingFee(fixedShippingFee)
                    .freeShippingThreshold(freeShippingThreshold)
                    .enabled(true)
                    .build();

            shippingFeeSettingsService.updateSettings(settingsDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật cài đặt phí ship thành công!");

        } catch (Exception e) {
            log.error("Error updating shipping settings: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật cài đặt phí ship: " + e.getMessage());
        }

        return "redirect:/admin/shipping-fees/list";
    }
}
