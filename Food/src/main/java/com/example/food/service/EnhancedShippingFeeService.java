package com.example.food.service;

import com.example.food.dto.ShippingCalculationResponse;
import com.example.food.dto.StoreSettingsDTO;
import com.example.food.model.StoreSettings;
import com.example.food.repository.StoreSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedShippingFeeService {
    
    private final StoreSettingsRepository storeSettingsRepository;
    private final ShippingFeeService shippingFeeService;
    
    /**
     * Tính phí ship dựa trên giá trị đơn hàng (đơn giản hóa)
     */
    public ShippingCalculationResponse calculateShippingFee(
            BigDecimal orderAmount, 
            String customerCity, 
            String customerDistrict) {
        
        try {
            // Sử dụng ShippingFeeService để tính phí ship dựa trên giá đơn hàng
            BigDecimal shippingFee = shippingFeeService.calculateShippingFee(orderAmount);
            
            String description = "Phí ship theo giá đơn hàng";
            if (shippingFee.compareTo(BigDecimal.ZERO) == 0) {
                description = "Miễn phí ship";
            }
            
            return createResponse(shippingFee, description);
                
        } catch (Exception e) {
            log.error("Error calculating shipping fee: ", e);
            // Fallback to default calculation
            return createResponse(BigDecimal.valueOf(15000), "Phí ship mặc định");
        }
    }
    
    /**
     * Lấy thông tin cửa hàng cho App
     */
    public StoreSettingsDTO getStoreSettings() {
        StoreSettings store = storeSettingsRepository.findByIsActiveTrue()
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cấu hình nhà hàng"));
        
        return StoreSettingsDTO.builder()
            .settingId(store.getSettingId())
            .storeName(store.getStoreName())
            .storePhone(store.getStorePhone())
            .storeEmail(store.getStoreEmail())
            .storeCity(store.getStoreCity())
            .storeDistrict(store.getStoreDistrict())
            .storeWard(store.getStoreWard())
            .storeStreet(store.getStoreStreet())
            .storeDescription(store.getStoreDescription())
            .isActive(store.getIsActive())
            .createdAt(store.getCreatedAt())
            .updatedAt(store.getUpdatedAt())
            .build();
    }

    /**
     * Tạo response cho shipping calculation
     */
    private ShippingCalculationResponse createResponse(BigDecimal shippingFee, String description) {
        return ShippingCalculationResponse.builder()
            .shippingFee(shippingFee)
            .distanceKm(BigDecimal.ZERO) // Không dùng khoảng cách nữa
            .estimatedDurationMinutes(30) // Ước tính 30 phút
            .fromCache(false)
            .description(description)
            .build();
    }
}
