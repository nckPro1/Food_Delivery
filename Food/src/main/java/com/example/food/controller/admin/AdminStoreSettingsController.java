package com.example.food.controller.admin;

import com.example.food.dto.ApiResponse;
import com.example.food.dto.StoreSettingsDTO;
import com.example.food.service.StoreSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/store-settings")
@RequiredArgsConstructor
@Slf4j
public class AdminStoreSettingsController {

    private final StoreSettingsService storeSettingsService;

    // ===============================
    // WEB PAGES (Thymeleaf)
    // ===============================

    /**
     * Trang cài đặt nhà hàng
     */
    @GetMapping
    public String storeSettingsPage(Model model) {
        try {
            StoreSettingsDTO settings = storeSettingsService.getActiveStoreSettings()
                    .orElse(new StoreSettingsDTO());
            
            model.addAttribute("settings", settings);
            model.addAttribute("pageTitle", "Cài đặt nhà hàng");
            return "admin/store-settings/form";
        } catch (Exception e) {
            log.error("Error loading store settings page: ", e);
            model.addAttribute("error", "Lỗi tải cài đặt nhà hàng: " + e.getMessage());
            return "admin/store-settings/form";
        }
    }

    // ===============================
    // API ENDPOINTS (JSON)
    // ===============================

    /**
     * Lấy cài đặt nhà hàng hiện tại (API)
     */
    @GetMapping("/api/current")
    @ResponseBody
    public ResponseEntity<ApiResponse<StoreSettingsDTO>> getCurrentStoreSettings() {
        try {
            StoreSettingsDTO settings = storeSettingsService.getActiveStoreSettings()
                    .orElse(new StoreSettingsDTO());

            return ResponseEntity.ok(ApiResponse.<StoreSettingsDTO>builder()
                    .success(true)
                    .message("Lấy cài đặt nhà hàng thành công")
                    .data(settings)
                    .build());
        } catch (Exception e) {
            log.error("Error getting current store settings: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<StoreSettingsDTO>builder()
                    .success(false)
                    .message("Lỗi lấy cài đặt nhà hàng: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Cập nhật cài đặt nhà hàng (API)
     */
    @PutMapping("/api/update")
    @ResponseBody
    public ResponseEntity<ApiResponse<StoreSettingsDTO>> updateStoreSettings(@RequestBody StoreSettingsDTO settingsDTO) {
        try {
            StoreSettingsDTO updated = storeSettingsService.updateStoreSettings(settingsDTO);

            return ResponseEntity.ok(ApiResponse.<StoreSettingsDTO>builder()
                    .success(true)
                    .message("Cập nhật cài đặt nhà hàng thành công")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Error updating store settings: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<StoreSettingsDTO>builder()
                    .success(false)
                    .message("Lỗi cập nhật cài đặt nhà hàng: " + e.getMessage())
                    .build());
        }
    }

}