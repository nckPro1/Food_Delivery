package com.example.food.controller.admin;

import com.example.food.dto.ShippingFeeSettingsDTO;
import com.example.food.dto.StoreSettingsDTO;
import com.example.food.service.StoreSettingsService;
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
public class AdminStoreSettingsController {

    private final StoreSettingsService storeSettingsService;
    private final ShippingFeeSettingsService shippingFeeSettingsService;

    // ===============================
    // STORE SETTINGS PAGES
    // ===============================

    /**
     * Trang cài đặt cửa hàng
     */
    @GetMapping("/store-settings")
    public String storeSettings(Model model) {
        log.info("Loading store settings page");

        // Lấy thông tin cửa hàng hiện tại
        Optional<StoreSettingsDTO> storeSettings = storeSettingsService.getCurrentSettings();
        model.addAttribute("storeSettings", storeSettings.orElse(new StoreSettingsDTO()));

        // Lấy cài đặt phí ship hiện tại
        Optional<ShippingFeeSettingsDTO> shippingSettings = shippingFeeSettingsService.getCurrentSettings();
        model.addAttribute("shippingSettings", shippingSettings.orElse(new ShippingFeeSettingsDTO()));

        model.addAttribute("pageTitle", "Cài đặt nhà hàng");
        return "admin/store-settings";
    }

    /**
     * Cập nhật thông tin cửa hàng
     */
    @PostMapping("/store-settings/update")
    public String updateStoreSettings(
            @RequestParam String storeName,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("Updating store settings");

            StoreSettingsDTO settingsDTO = StoreSettingsDTO.builder()
                    .storeName(storeName)
                    .phoneNumber(phoneNumber)
                    .email(email)
                    .address(address)
                    .description(description)
                    .enabled(true)
                    .build();

            storeSettingsService.updateSettings(settingsDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin cửa hàng thành công!");

        } catch (Exception e) {
            log.error("Error updating store settings: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật thông tin cửa hàng: " + e.getMessage());
        }

        return "redirect:/admin/store-settings";
    }

    /**
     * Cập nhật cài đặt phí ship
     */
    @PostMapping("/shipping-settings/update")
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

        return "redirect:/admin/store-settings";
    }
}
