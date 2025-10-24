package com.example.food.service;

import com.example.food.dto.ShippingFeeDTO;
import com.example.food.model.ShippingFee;
import com.example.food.repository.ShippingFeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingFeeService {

    private final ShippingFeeRepository shippingFeeRepository;

    /**
     * Tính phí ship cho đơn hàng dựa trên giá trị đơn hàng
     */
    public BigDecimal calculateShippingFee(BigDecimal orderAmount) {
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Tìm shipping fee phù hợp nhất
        Optional<ShippingFee> bestFee = shippingFeeRepository.findBestApplicableShippingFee(orderAmount);
        
        if (bestFee.isPresent()) {
            return bestFee.get().calculateShippingFee(orderAmount);
        }

        // Fallback: tìm default shipping fee
        Optional<ShippingFee> defaultFee = shippingFeeRepository.findDefaultActiveShippingFee();
        if (defaultFee.isPresent()) {
            return defaultFee.get().calculateShippingFee(orderAmount);
        }

        // Fallback cuối cùng: 15k
        log.warn("No shipping fee configuration found, using default 15k");
        return BigDecimal.valueOf(15000);
    }

    /**
     * Lấy thông tin shipping fee mặc định
     */
    public ShippingFeeInfo getDefaultShippingFeeInfo() {
        Optional<ShippingFee> defaultFee = shippingFeeRepository.findDefaultActiveShippingFee();
        
        if (defaultFee.isPresent()) {
            ShippingFee fee = defaultFee.get();
            return ShippingFeeInfo.builder()
                    .defaultShippingFee(fee.getFeeAmount())
                    .freeShippingThreshold(fee.getFreeShippingThreshold())
                    .minOrderAmount(fee.getMinOrderAmount())
                    .build();
        }

        // Fallback values
        return ShippingFeeInfo.builder()
                .defaultShippingFee(BigDecimal.valueOf(15000))
                .freeShippingThreshold(BigDecimal.valueOf(200000))
                .minOrderAmount(BigDecimal.ZERO)
                .build();
    }

    /**
     * Lấy tất cả shipping fees active
     */
    public List<ShippingFee> getAllActiveShippingFees() {
        return shippingFeeRepository.findAllActiveOrderByDefaultAndMinAmount();
    }

    /**
     * Lấy tất cả shipping fees (DTO)
     */
    public List<ShippingFeeDTO> getAllShippingFees() {
        return shippingFeeRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy shipping fee theo ID (DTO)
     */
    public Optional<ShippingFeeDTO> getShippingFeeById(Long id) {
        return shippingFeeRepository.findById(id)
                .map(this::convertToDTO);
    }

    /**
     * Tạo shipping fee mới (DTO)
     */
    @Transactional
    public ShippingFeeDTO createShippingFee(ShippingFeeDTO shippingFeeDTO) {
        ShippingFee shippingFee = convertToEntity(shippingFeeDTO);
        ShippingFee saved = shippingFeeRepository.save(shippingFee);
        return convertToDTO(saved);
    }

    /**
     * Cập nhật shipping fee (DTO)
     */
    @Transactional
    public ShippingFeeDTO updateShippingFee(Long id, ShippingFeeDTO shippingFeeDTO) {
        ShippingFee existing = shippingFeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Shipping fee not found with ID: " + id));
        
        // Update fields
        existing.setFeeName(shippingFeeDTO.getFeeName());
        existing.setFeeAmount(shippingFeeDTO.getFeeAmount());
        existing.setMinOrderAmount(shippingFeeDTO.getMinOrderAmount());
        existing.setMaxOrderAmount(shippingFeeDTO.getMaxOrderAmount());
        existing.setFreeShippingThreshold(shippingFeeDTO.getFreeShippingThreshold());
        existing.setPerKmFee(shippingFeeDTO.getPerKmFee());
        existing.setIsDefault(shippingFeeDTO.getIsDefault());
        existing.setIsActive(shippingFeeDTO.getIsActive());
        existing.setDescription(shippingFeeDTO.getDescription());
        
        ShippingFee saved = shippingFeeRepository.save(existing);
        return convertToDTO(saved);
    }

    /**
     * Xóa shipping fee (soft delete)
     */
    @Transactional
    public void deleteShippingFee(Long id) {
        Optional<ShippingFee> shippingFee = shippingFeeRepository.findById(id);
        if (shippingFee.isPresent()) {
            ShippingFee fee = shippingFee.get();
            fee.setIsActive(false);
            shippingFeeRepository.save(fee);
        }
    }

    /**
     * Đặt làm shipping fee mặc định
     */
    @Transactional
    public void setDefaultShippingFee(Long id) {
        // Remove default from all others
        List<ShippingFee> allFees = shippingFeeRepository.findAll();
        allFees.forEach(fee -> fee.setIsDefault(false));
        shippingFeeRepository.saveAll(allFees);
        
        // Set new default
        ShippingFee newDefault = shippingFeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Shipping fee not found with ID: " + id));
        newDefault.setIsDefault(true);
        shippingFeeRepository.save(newDefault);
    }

    /**
     * Convert Entity to DTO
     */
    private ShippingFeeDTO convertToDTO(ShippingFee shippingFee) {
        return ShippingFeeDTO.builder()
                .shippingFeeId(shippingFee.getShippingFeeId())
                .feeName(shippingFee.getFeeName())
                .feeAmount(shippingFee.getFeeAmount())
                .minOrderAmount(shippingFee.getMinOrderAmount())
                .maxOrderAmount(shippingFee.getMaxOrderAmount())
                .freeShippingThreshold(shippingFee.getFreeShippingThreshold())
                .perKmFee(shippingFee.getPerKmFee())
                .isDefault(shippingFee.getIsDefault())
                .isActive(shippingFee.getIsActive())
                .description(shippingFee.getDescription())
                .createdAt(shippingFee.getCreatedAt())
                .updatedAt(shippingFee.getUpdatedAt())
                .build();
    }

    /**
     * Convert DTO to Entity
     */
    private ShippingFee convertToEntity(ShippingFeeDTO shippingFeeDTO) {
        return ShippingFee.builder()
                .feeName(shippingFeeDTO.getFeeName())
                .feeAmount(shippingFeeDTO.getFeeAmount())
                .minOrderAmount(shippingFeeDTO.getMinOrderAmount())
                .maxOrderAmount(shippingFeeDTO.getMaxOrderAmount())
                .freeShippingThreshold(shippingFeeDTO.getFreeShippingThreshold())
                .perKmFee(shippingFeeDTO.getPerKmFee())
                .isDefault(shippingFeeDTO.getIsDefault())
                .isActive(shippingFeeDTO.getIsActive())
                .description(shippingFeeDTO.getDescription())
                .build();
    }

    // Inner class for shipping fee info
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ShippingFeeInfo {
        private BigDecimal defaultShippingFee;
        private BigDecimal freeShippingThreshold;
        private BigDecimal minOrderAmount;
    }
}
