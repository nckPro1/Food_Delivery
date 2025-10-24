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
