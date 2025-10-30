package com.example.food.repository;

import com.example.food.model.StoreSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreSettingsRepository extends JpaRepository<StoreSettings, Long> {
    
    /**
     * Tìm cài đặt cửa hàng đang được kích hoạt
     */
    Optional<StoreSettings> findByEnabledTrue();
}
