package com.example.food.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingCalculationResponse {
    private BigDecimal shippingFee;
    private BigDecimal distanceKm;
    private Integer estimatedDurationMinutes;
    private boolean fromCache;
    private String description;
    
    // Backward compatibility methods
    public BigDecimal getDistance() { 
        return distanceKm; 
    }
    
    public void setDistance(BigDecimal distance) { 
        this.distanceKm = distance; 
    }
    
    public String getShippingFeeDescription() { 
        return description; 
    }
    
    public void setShippingFeeDescription(String shippingFeeDescription) { 
        this.description = shippingFeeDescription; 
    }
    
    public boolean isDeliverable() { 
        return shippingFee != null && shippingFee.compareTo(BigDecimal.ZERO) >= 0; 
    }
}
