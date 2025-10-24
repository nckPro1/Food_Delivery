package com.example.food.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatsDTO {

    private Long totalOrders;
    private Long pendingOrders;
    private Long confirmedOrders;
    private Long deliveringOrders;
    private Long completedOrders;

    private BigDecimal totalRevenue;
    private BigDecimal todayRevenue;
    private BigDecimal thisMonthRevenue;

    private Map<String, Long> ordersByStatus;
    private Map<String, BigDecimal> revenueByMonth;

    // Helper methods
    public Long getActiveOrders() {
        return pendingOrders + confirmedOrders + deliveringOrders;
    }

    public BigDecimal getCompletionRate() {
        if (totalOrders == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(completedOrders).divide(BigDecimal.valueOf(totalOrders), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public BigDecimal getCancellationRate() {
        // Since we removed cancelled status, return 0
        return BigDecimal.ZERO;
    }
}
