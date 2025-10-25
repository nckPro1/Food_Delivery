package com.example.food.controller;

import com.example.food.dto.ApiResponse;
import com.example.food.dto.ShippingCalculationRequest;
import com.example.food.dto.ShippingCalculationResponse;
import com.example.food.service.EnhancedShippingFeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
@Slf4j
public class AppLocationController {

    private final EnhancedShippingFeeService enhancedShippingFeeService;

    /**
     * API cho App: Tính phí ship dựa trên địa chỉ giao hàng
     */
    @PostMapping("/calculate-shipping")
    public ResponseEntity<ApiResponse<ShippingCalculationResponse>> calculateShipping(
            @RequestBody ShippingCalculationRequest request) {
        try {
            log.info("App requesting shipping calculation for address: {}, {}", 
                    request.getDeliveryCity(), request.getDeliveryDistrict());

            ShippingCalculationResponse response = enhancedShippingFeeService.calculateShippingFee(
                request.getOrderAmount(),
                request.getDeliveryCity(),
                request.getDeliveryDistrict()
            );

            return ResponseEntity.ok(ApiResponse.<ShippingCalculationResponse>builder()
                .success(true)
                .message("Tính phí ship thành công")
                .data(response)
                .build());

        } catch (Exception e) {
            log.error("Error calculating shipping fee for app: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<ShippingCalculationResponse>builder()
                .success(false)
                .message("Lỗi tính phí ship: " + e.getMessage())
                .build());
        }
    }

    /**
     * API cho App: Kiểm tra địa chỉ có thể giao hàng không
     */
    @PostMapping("/check-delivery-availability")
    public ResponseEntity<ApiResponse<DeliveryAvailabilityResponse>> checkDeliveryAvailability(
            @RequestBody DeliveryLocationRequest request) {
        try {
            log.info("App checking delivery availability for address: {}, {}", 
                    request.getCity(), request.getDistrict());

            // Tính phí ship để kiểm tra
            ShippingCalculationResponse shippingResponse = enhancedShippingFeeService.calculateShippingFee(
                BigDecimal.valueOf(100000), // Test với 100k
                request.getCity(),
                request.getDistrict()
            );

            boolean isDeliverable = shippingResponse != null && 
                                  shippingResponse.getShippingFee().compareTo(BigDecimal.ZERO) > 0;

            DeliveryAvailabilityResponse response = DeliveryAvailabilityResponse.builder()
                .isDeliverable(isDeliverable)
                .shippingFee(shippingResponse != null ? shippingResponse.getShippingFee() : BigDecimal.ZERO)
                .estimatedDurationMinutes(shippingResponse != null ? shippingResponse.getEstimatedDurationMinutes() : 0)
                .message(isDeliverable ? "Có thể giao hàng" : "Không thể giao hàng")
                .build();

            return ResponseEntity.ok(ApiResponse.<DeliveryAvailabilityResponse>builder()
                .success(true)
                .message("Kiểm tra khả năng giao hàng thành công")
                .data(response)
                .build());

        } catch (Exception e) {
            log.error("Error checking delivery availability: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<DeliveryAvailabilityResponse>builder()
                .success(false)
                .message("Lỗi kiểm tra khả năng giao hàng: " + e.getMessage())
                .build());
        }
    }

    /**
     * API cho App: Lấy danh sách các thành phố có sẵn
     */
    @GetMapping("/cities")
    public ResponseEntity<ApiResponse<java.util.List<String>>> getAvailableCities() {
        try {
            // Danh sách các thành phố chính của Việt Nam
            java.util.List<String> cities = java.util.Arrays.asList(
                "ho-chi-minh",
                "ha-noi", 
                "da-nang",
                "hai-phong",
                "can-tho",
                "bien-hoa",
                "nha-trang",
                "hue",
                "buon-ma-thuot",
                "quy-nhon",
                "vung-tau",
                "thai-nguyen",
                "nam-dinh",
                "thanh-hoa",
                "long-xuyen",
                "my-tho",
                "ca-mau",
                "rach-gia",
                "soc-trang",
                "bac-lieu"
            );

            return ResponseEntity.ok(ApiResponse.<java.util.List<String>>builder()
                .success(true)
                .message("Lấy danh sách thành phố thành công")
                .data(cities)
                .build());

        } catch (Exception e) {
            log.error("Error getting cities: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<java.util.List<String>>builder()
                .success(false)
                .message("Lỗi lấy danh sách thành phố: " + e.getMessage())
                .build());
        }
    }

    /**
     * API cho App: Lấy danh sách các quận/huyện theo thành phố
     */
    @GetMapping("/districts")
    public ResponseEntity<ApiResponse<java.util.List<String>>> getDistrictsByCity(
            @RequestParam String city) {
        try {
            java.util.List<String> districts = new java.util.ArrayList<>();
            
            switch (city.toLowerCase()) {
                case "ho-chi-minh":
                    districts = java.util.Arrays.asList(
                        "quan-1", "quan-2", "quan-3", "quan-4", "quan-5", "quan-6", "quan-7", "quan-8", "quan-9", "quan-10",
                        "quan-11", "quan-12", "quan-thu-duc", "quan-go-vap", "quan-binh-thanh", "quan-tan-binh", "quan-tan-phu",
                        "quan-phu-nhuan", "quan-binh-tan", "quan-hoc-mon", "quan-cu-chi", "quan-binh-chanh", "quan-nha-be", "quan-can-gio"
                    );
                    break;
                case "ha-noi":
                    districts = java.util.Arrays.asList(
                        "quan-hoan-kiem", "quan-ba-dinh", "quan-dong-da", "quan-hai-ba-trung", "quan-hoang-mai", "quan-thanh-xuan",
                        "quan-long-bien", "quan-cau-giay", "quan-dong-anh", "quan-gia-lam", "quan-ha-dong", "quan-hai-ba-trung",
                        "quan-hoang-mai", "quan-long-bien", "quan-nam-tu-liem", "quan-thanh-tri", "quan-thuong-tin", "quan-ung-hoa"
                    );
                    break;
                case "da-nang":
                    districts = java.util.Arrays.asList(
                        "quan-hai-chau", "quan-thanh-khe", "quan-son-tra", "quan-ngu-hanh-son", "quan-lien-chieu", "quan-cam-le",
                        "huyen-hoa-vang", "huyen-hoang-sa"
                    );
                    break;
                default:
                    // Mặc định cho các thành phố khác
                    districts = java.util.Arrays.asList(
                        "quan-trung-tam", "quan-1", "quan-2", "quan-3", "quan-4", "quan-5",
                        "huyen-1", "huyen-2", "huyen-3", "huyen-4", "huyen-5"
                    );
                    break;
            }

            return ResponseEntity.ok(ApiResponse.<java.util.List<String>>builder()
                .success(true)
                .message("Lấy danh sách quận/huyện thành công")
                .data(districts)
                .build());

        } catch (Exception e) {
            log.error("Error getting districts: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<java.util.List<String>>builder()
                .success(false)
                .message("Lỗi lấy danh sách quận/huyện: " + e.getMessage())
                .build());
        }
    }

    /**
     * API cho App: Lấy danh sách các phường/xã theo quận/huyện
     */
    @GetMapping("/wards")
    public ResponseEntity<ApiResponse<java.util.List<String>>> getWardsByDistrict(
            @RequestParam String city,
            @RequestParam String district) {
        try {
            java.util.List<String> wards = new java.util.ArrayList<>();
            
            // Mặc định cho tất cả quận/huyện
            if (district.startsWith("quan-")) {
                wards = java.util.Arrays.asList(
                    "phuong-1", "phuong-2", "phuong-3", "phuong-4", "phuong-5", "phuong-6", "phuong-7", "phuong-8", "phuong-9", "phuong-10",
                    "phuong-trung-tam", "phuong-dong", "phuong-tay", "phuong-nam", "phuong-bac"
                );
            } else if (district.startsWith("huyen-")) {
                wards = java.util.Arrays.asList(
                    "xa-trung-tam", "xa-1", "xa-2", "xa-3", "xa-4", "xa-5", "xa-6", "xa-7", "xa-8", "xa-9", "xa-10"
                );
            }

            return ResponseEntity.ok(ApiResponse.<java.util.List<String>>builder()
                .success(true)
                .message("Lấy danh sách phường/xã thành công")
                .data(wards)
                .build());

        } catch (Exception e) {
            log.error("Error getting wards: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<java.util.List<String>>builder()
                .success(false)
                .message("Lỗi lấy danh sách phường/xã: " + e.getMessage())
                .build());
        }
    }

    /**
     * API cho App: Lấy thông tin cửa hàng
     */
    @GetMapping("/store-info")
    public ResponseEntity<ApiResponse<StoreInfoResponse>> getStoreInfo() {
        try {
            // Lấy thông tin cửa hàng từ service
            var storeSettings = enhancedShippingFeeService.getStoreSettings();
            
            StoreInfoResponse response = StoreInfoResponse.builder()
                .storeName(storeSettings.getStoreName())
                .storePhone(storeSettings.getStorePhone())
                .storeEmail(storeSettings.getStoreEmail())
                .storeCity(storeSettings.getStoreCity())
                .storeDistrict(storeSettings.getStoreDistrict())
                .storeWard(storeSettings.getStoreWard())
                .storeStreet(storeSettings.getStoreStreet())
                .storeDescription(storeSettings.getStoreDescription())
                .build();

            return ResponseEntity.ok(ApiResponse.<StoreInfoResponse>builder()
                .success(true)
                .message("Lấy thông tin cửa hàng thành công")
                .data(response)
                .build());

        } catch (Exception e) {
            log.error("Error getting store info: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<StoreInfoResponse>builder()
                .success(false)
                .message("Lỗi lấy thông tin cửa hàng: " + e.getMessage())
                .build());
        }
    }

    // DTOs for API responses
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DeliveryAvailabilityResponse {
        private boolean isDeliverable;
        private BigDecimal shippingFee;
        private Integer estimatedDurationMinutes;
        private String message;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class StoreInfoResponse {
        private String storeName;
        private String storePhone;
        private String storeEmail;
        private String storeCity;
        private String storeDistrict;
        private String storeWard;
        private String storeStreet;
        private String storeDescription;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DeliveryLocationRequest {
        private String city;
        private String district;
        private String ward;
        private String street;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NearbySearchRequest {
        private String city;
        private String district;
        private String ward;
        private String street;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NearbyAddressesResponse {
        private java.util.List<NearbyAddress> addresses;
        private Integer totalCount;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NearbyAddress {
        private String address;
        private String city;
        private String district;
        private String ward;
        private String street;
        private BigDecimal shippingFee;
        private Integer estimatedDurationMinutes;
    }
}
