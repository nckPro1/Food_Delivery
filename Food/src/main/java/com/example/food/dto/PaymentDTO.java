package com.example.food.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {

    private Long paymentId;
    private Long orderId;
    private String paymentMethod;
    private LocalDateTime paymentDate;
    private BigDecimal paymentAmount;
    private String paymentStatus;
    private String transactionId;
    private String paymentNotes;
    private LocalDateTime createdAt;

    // Helper methods
    public boolean isPaid() {
        return "COMPLETED".equalsIgnoreCase(paymentStatus);
    }

    public boolean isFailed() {
        return "FAILED".equalsIgnoreCase(paymentStatus);
    }
}
