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
public class StoreSettingsService {

    private final StoreSettingsRepository storeSettingsRepository;

    public Optional<StoreSettingsDTO> getActiveStoreSettings() {
        return storeSettingsRepository.findByIsActiveTrue()
                .map(this::convertToDTO);
    }

    public StoreSettingsDTO getDefaultStoreSettings() {
        return storeSettingsRepository.findByIsActiveTrue()
                .map(this::convertToDTO)
                .orElse(getDefaultSettings());
    }

    @Transactional
    public StoreSettingsDTO updateStoreSettings(StoreSettingsDTO dto) {
        StoreSettings settings = storeSettingsRepository.findByIsActiveTrue()
                .orElse(StoreSettings.builder().build());

        // Update fields
        settings.setStoreName(dto.getStoreName());
        settings.setStorePhone(dto.getStorePhone());
        settings.setStoreEmail(dto.getStoreEmail());
        settings.setStoreCity(dto.getStoreCity());
        settings.setStoreDistrict(dto.getStoreDistrict());
        settings.setStoreWard(dto.getStoreWard());
        settings.setStoreStreet(dto.getStoreStreet());
        settings.setStoreDescription(dto.getStoreDescription());
        settings.setIsActive(true);

        StoreSettings savedSettings = storeSettingsRepository.save(settings);
        log.info("Store settings updated: {}", savedSettings.getSettingId());

        return convertToDTO(savedSettings);
    }

    private StoreSettingsDTO convertToDTO(StoreSettings settings) {
        return StoreSettingsDTO.builder()
                .settingId(settings.getSettingId())
                .storeName(settings.getStoreName())
                .storePhone(settings.getStorePhone())
                .storeEmail(settings.getStoreEmail())
                .storeCity(settings.getStoreCity())
                .storeDistrict(settings.getStoreDistrict())
                .storeWard(settings.getStoreWard())
                .storeStreet(settings.getStoreStreet())
                .storeDescription(settings.getStoreDescription())
                .isActive(settings.getIsActive())
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    private StoreSettingsDTO getDefaultSettings() {
        return StoreSettingsDTO.builder()
                .storeName("Nhà hàng của tôi")
                .storePhone("0123456789")
                .storeEmail("info@myrestaurant.com")
                .storeCity("ho-chi-minh")
                .storeDistrict("quan-1")
                .storeWard("Phường Bến Nghé")
                .storeStreet("123 Nguyễn Huệ")
                .storeDescription("Nhà hàng phục vụ các món ăn ngon và chất lượng")
                .isActive(true)
                .build();
    }

}
