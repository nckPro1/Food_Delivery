package com.example.food.controller.admin;

import com.example.food.dto.ApiResponse;
import com.example.food.dto.ShippingFeeSettingsDTO;
import com.example.food.service.ShippingFeeSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/shipping-fees")
@RequiredArgsConstructor
@Slf4j
public class AdminShippingFeeController {

    private final ShippingFeeSettingsService shippingFeeSettingsService;

    /**
     * Trang cài đặt phí ship
     */
    @GetMapping
    public String shippingFeePage(Model model) {
        try {
            ShippingFeeSettingsDTO settings = shippingFeeSettingsService.getCurrentSettings()
                    .orElse(new ShippingFeeSettingsDTO());
            
            model.addAttribute("settings", settings);
            model.addAttribute("pageTitle", "Cài đặt phí ship");
            return "admin/shipping-fees/simple";
        } catch (Exception e) {
            log.error("Error loading shipping fee page: ", e);
            model.addAttribute("error", "Lỗi tải cài đặt phí ship: " + e.getMessage());
            return "admin/shipping-fees/simple";
        }
    }

    /**
     * Lấy cài đặt phí ship hiện tại (API)
     */
    @GetMapping("/api/current")
    @ResponseBody
    public ResponseEntity<ApiResponse<ShippingFeeSettingsDTO>> getCurrentSettings() {
        try {
            ShippingFeeSettingsDTO settings = shippingFeeSettingsService.getCurrentSettings()
                    .orElse(new ShippingFeeSettingsDTO());

            return ResponseEntity.ok(ApiResponse.<ShippingFeeSettingsDTO>builder()
                    .success(true)
                    .message("Lấy cài đặt phí ship thành công")
                    .data(settings)
                    .build());
        } catch (Exception e) {
            log.error("Error getting current shipping fee settings: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<ShippingFeeSettingsDTO>builder()
                    .success(false)
                    .message("Lỗi lấy cài đặt phí ship: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Cập nhật cài đặt phí ship (API)
     */
    @PutMapping("/api/update")
    @ResponseBody
    public ResponseEntity<ApiResponse<ShippingFeeSettingsDTO>> updateSettings(@RequestBody ShippingFeeSettingsDTO settingsDTO) {
        try {
            ShippingFeeSettingsDTO updated = shippingFeeSettingsService.updateSettings(settingsDTO);

            return ResponseEntity.ok(ApiResponse.<ShippingFeeSettingsDTO>builder()
                    .success(true)
                    .message("Cập nhật cài đặt phí ship thành công")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Error updating shipping fee settings: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<ShippingFeeSettingsDTO>builder()
                    .success(false)
                    .message("Lỗi cập nhật cài đặt phí ship: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Test phí ship (API)
     */
    @GetMapping("/api/test")
    @ResponseBody
    public ResponseEntity<ApiResponse<Object>> testShippingFee(
            @RequestParam String city,
            @RequestParam String district) {
        try {
            var result = shippingFeeSettingsService.calculateShippingFee(city, district);

            return ResponseEntity.ok(ApiResponse.<Object>builder()
                    .success(true)
                    .message("Test phí ship thành công")
                    .data(result)
                    .build());
        } catch (Exception e) {
            log.error("Error testing shipping fee: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Object>builder()
                    .success(false)
                    .message("Lỗi test phí ship: " + e.getMessage())
                    .build());
        }
    }
}