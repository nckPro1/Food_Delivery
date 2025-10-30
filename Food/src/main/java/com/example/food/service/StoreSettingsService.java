package com.example.food.service;

import com.example.food.dto.StoreSettingsDTO;
import com.example.food.model.StoreSettings;
import com.example.food.repository.StoreSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StoreSettingsService {

    private final StoreSettingsRepository storeSettingsRepository;

    /**
     * Lấy thông tin cửa hàng hiện tại
     */
    @Transactional(readOnly = true)
    public Optional<StoreSettingsDTO> getCurrentSettings() {
        log.info("Fetching current store settings");
        return storeSettingsRepository.findByEnabledTrue()
                .map(this::convertToDTO);
    }

    /**
     * Cập nhật thông tin cửa hàng
     */
    @Transactional
    public StoreSettingsDTO updateSettings(StoreSettingsDTO settingsDTO) {
        log.info("Updating store settings");

        Optional<StoreSettings> existingSettings = storeSettingsRepository.findByEnabledTrue();

        StoreSettings settings;
        if (existingSettings.isPresent()) {
            // Cập nhật cài đặt hiện tại
            settings = existingSettings.get();
            settings.setStoreName(settingsDTO.getStoreName());
            settings.setPhoneNumber(settingsDTO.getPhoneNumber());
            settings.setEmail(settingsDTO.getEmail());
            settings.setAddress(settingsDTO.getAddress());
            settings.setDescription(settingsDTO.getDescription());
        } else {
            // Tạo cài đặt mới
            settings = StoreSettings.builder()
                    .storeName(settingsDTO.getStoreName())
                    .phoneNumber(settingsDTO.getPhoneNumber())
                    .email(settingsDTO.getEmail())
                    .address(settingsDTO.getAddress())
                    .description(settingsDTO.getDescription())
                    .enabled(true)
                    .build();
        }

        StoreSettings savedSettings = storeSettingsRepository.save(settings);
        return convertToDTO(savedSettings);
    }

    /**
     * Lấy thông tin cửa hàng cho API
     */
    @Transactional(readOnly = true)
    public StoreSettingsDTO getStoreInfo() {
        log.info("Fetching store info for API");
        return storeSettingsRepository.findByEnabledTrue()
                .map(this::convertToDTO)
                .orElse(StoreSettingsDTO.builder()
                        .storeName("FoodieExpress")
                        .phoneNumber("0123456789")
                        .email("contact@foodieexpress.com")
                        .address("123 Đường ABC, Quận XYZ, TP.HCM")
                        .description("Cửa hàng thức ăn nhanh uy tín")
                        .build());
    }

    /**
     * Chuyển đổi entity sang DTO
     */
    private StoreSettingsDTO convertToDTO(StoreSettings settings) {
        return StoreSettingsDTO.builder()
                .id(settings.getId())
                .storeName(settings.getStoreName())
                .phoneNumber(settings.getPhoneNumber())
                .email(settings.getEmail())
                .address(settings.getAddress())
                .description(settings.getDescription())
                .enabled(settings.getEnabled())
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}
