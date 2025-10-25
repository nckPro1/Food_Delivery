package com.example.food.repository;

import com.example.food.model.ShippingFeeSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShippingFeeSettingsRepository extends JpaRepository<ShippingFeeSettings, Long> {
    
    Optional<ShippingFeeSettings> findFirstByOrderByCreatedAtDesc();
}

