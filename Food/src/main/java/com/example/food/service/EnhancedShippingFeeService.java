package com.example.food.service;

import com.example.food.dto.ShippingCalculationResponse;
import com.example.food.dto.StoreSettingsDTO;
import com.example.food.model.StoreSettings;
import com.example.food.repository.StoreSettingsRepository;
import com.example.food.repository.ShippingFeeSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedShippingFeeService {
    
    private final StoreSettingsRepository storeSettingsRepository;
    private final ShippingFeeSettingsRepository shippingFeeSettingsRepository;
    
    /**
     * Tính phí ship dựa trên khu vực (thành phố, quận)
     */
    public ShippingCalculationResponse calculateShippingFee(
            BigDecimal orderAmount, 
            String customerCity, 
            String customerDistrict) {
        
        try {
            // 1. Lấy cấu hình phí ship
            var shippingSettings = shippingFeeSettingsRepository.findFirstByOrderByCreatedAtDesc();
            if (shippingSettings.isEmpty() || !shippingSettings.get().getEnabled()) {
                return createResponse(BigDecimal.ZERO, "Phí ship tạm dừng");
            }
            
            var settings = shippingSettings.get();
            
            // 2. Lấy thông tin nhà hàng
            StoreSettings store = storeSettingsRepository.findByIsActiveTrue()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cấu hình nhà hàng"));
            
            // 3. Tính phí ship dựa trên khu vực
            BigDecimal shippingFee;
            String description;
            
            if (customerCity.equals(store.getStoreCity())) {
                if (customerDistrict.equals(store.getStoreDistrict())) {
                    // Cùng quận
                    shippingFee = BigDecimal.valueOf(settings.getSameDistrictFee());
                    description = "Cùng quận";
                } else {
                    // Khác quận
                    shippingFee = BigDecimal.valueOf(settings.getDifferentDistrictFee());
                    description = "Khác quận";
                }
            } else {
                // Ngoài thành phố
                shippingFee = BigDecimal.valueOf(settings.getOutsideCityFee());
                description = shippingFee.compareTo(BigDecimal.ZERO) == 0 ? "Không nhận đơn" : "Ngoài thành phố";
            }
            
            // 4. Kiểm tra miễn phí ship
            if (orderAmount.compareTo(BigDecimal.valueOf(settings.getFreeShippingThreshold())) >= 0) {
                shippingFee = BigDecimal.ZERO;
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
