package com.example.food.service;

import com.example.food.dto.ShippingFeeSettingsDTO;
import com.example.food.model.ShippingFeeSettings;
import com.example.food.repository.ShippingFeeSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingFeeSettingsService {

    private final ShippingFeeSettingsRepository repository;

    public Optional<ShippingFeeSettingsDTO> getCurrentSettings() {
        return repository.findFirstByOrderByCreatedAtDesc()
                .map(this::convertToDTO);
    }

    @Transactional
    public ShippingFeeSettingsDTO updateSettings(ShippingFeeSettingsDTO dto) {
        ShippingFeeSettings settings = repository.findFirstByOrderByCreatedAtDesc()
                .orElse(ShippingFeeSettings.builder().build());

        // Update fields
        settings.setSameDistrictFee(dto.getSameDistrictFee());
        settings.setDifferentDistrictFee(dto.getDifferentDistrictFee());
        settings.setOutsideCityFee(dto.getOutsideCityFee());
        settings.setFreeShippingThreshold(dto.getFreeShippingThreshold());
        settings.setEnabled(dto.getEnabled());

        ShippingFeeSettings savedSettings = repository.save(settings);
        log.info("Shipping fee settings updated: {}", savedSettings.getId());

        return convertToDTO(savedSettings);
    }

    private ShippingFeeSettingsDTO convertToDTO(ShippingFeeSettings settings) {
        return ShippingFeeSettingsDTO.builder()
                .id(settings.getId())
                .sameDistrictFee(settings.getSameDistrictFee())
                .differentDistrictFee(settings.getDifferentDistrictFee())
                .outsideCityFee(settings.getOutsideCityFee())
                .freeShippingThreshold(settings.getFreeShippingThreshold())
                .enabled(settings.getEnabled())
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    /**
     * Tính phí ship dựa trên thành phố và quận
     */
    public Map<String, Object> calculateShippingFee(String customerCity, String customerDistrict) {
        ShippingFeeSettings settings = repository.findFirstByOrderByCreatedAtDesc()
                .orElse(ShippingFeeSettings.builder().build());

        if (!settings.getEnabled()) {
            return Map.of(
                "fee", 0,
                "description", "Phí ship tạm dừng",
                "type", "disabled"
            );
        }

        // Lấy thông tin nhà hàng từ store settings
        String storeCity = "ho-chi-minh"; // Default, có thể lấy từ store settings
        String storeDistrict = "quan-1"; // Default, có thể lấy từ store settings

        int fee;
        String description;
        String type;

        if (customerCity.equals(storeCity)) {
            if (customerDistrict.equals(storeDistrict)) {
                // Cùng quận
                fee = settings.getSameDistrictFee();
                description = "Cùng quận";
                type = "same_district";
            } else {
                // Khác quận
                fee = settings.getDifferentDistrictFee();
                description = "Khác quận";
                type = "different_district";
            }
        } else {
            // Ngoài thành phố
            fee = settings.getOutsideCityFee();
            description = fee == 0 ? "Không nhận đơn" : "Ngoài thành phố";
            type = "outside_city";
        }

        Map<String, Object> result = new HashMap<>();
        result.put("fee", fee);
        result.put("description", description);
        result.put("type", type);
        result.put("freeShippingThreshold", settings.getFreeShippingThreshold());

        return result;
    }
}
