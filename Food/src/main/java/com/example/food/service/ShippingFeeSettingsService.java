package com.example.food.service;

import com.example.food.dto.ShippingFeeSettingsDTO;
import com.example.food.model.ShippingFeeSettings;
import com.example.food.repository.ShippingFeeSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ShippingFeeSettingsService {

    private final ShippingFeeSettingsRepository shippingFeeSettingsRepository;

    /**
     * Lấy cài đặt phí ship hiện tại
     */
    @Transactional(readOnly = true)
    public Optional<ShippingFeeSettingsDTO> getCurrentSettings() {
        log.info("Fetching current shipping fee settings");
        return shippingFeeSettingsRepository.findByEnabledTrue()
                .map(this::convertToDTO);
    }

    /**
     * Tính phí ship cho đơn hàng
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateShippingFee(BigDecimal orderAmount) {
        log.info("Calculating shipping fee for order amount: {}", orderAmount);

        Optional<ShippingFeeSettings> settings = shippingFeeSettingsRepository.findByEnabledTrue();
        if (settings.isEmpty()) {
            // Nếu không có cài đặt, trả về 0
            return BigDecimal.ZERO;
        }

        return settings.get().calculateShippingFee(orderAmount);
    }

    /**
     * Cập nhật cài đặt phí ship
     */
    @Transactional
    public ShippingFeeSettingsDTO updateSettings(ShippingFeeSettingsDTO settingsDTO) {
        log.info("Updating shipping fee settings");

        Optional<ShippingFeeSettings> existingSettings = shippingFeeSettingsRepository.findByEnabledTrue();

        ShippingFeeSettings settings;
        if (existingSettings.isPresent()) {
            // Cập nhật cài đặt hiện tại
            settings = existingSettings.get();
            settings.setFixedShippingFee(settingsDTO.getFixedShippingFee());
            settings.setFreeShippingThreshold(settingsDTO.getFreeShippingThreshold());
        } else {
            // Tạo cài đặt mới
            settings = ShippingFeeSettings.builder()
                    .fixedShippingFee(settingsDTO.getFixedShippingFee())
                    .freeShippingThreshold(settingsDTO.getFreeShippingThreshold())
                    .enabled(true)
                    .build();
        }

        ShippingFeeSettings savedSettings = shippingFeeSettingsRepository.save(settings);
        return convertToDTO(savedSettings);
    }

    /**
     * Chuyển đổi entity sang DTO
     */
    private ShippingFeeSettingsDTO convertToDTO(ShippingFeeSettings settings) {
        return ShippingFeeSettingsDTO.builder()
                .id(settings.getId())
                .fixedShippingFee(settings.getFixedShippingFee())
                .freeShippingThreshold(settings.getFreeShippingThreshold())
                .enabled(settings.getEnabled())
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}
