package com.example.food.dto;

import com.example.food.model.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private String userPhone;
    private Order.OrderStatus orderStatus;
    private BigDecimal totalAmount;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private Order.PaymentStatus paymentStatus;
    private Order.PaymentMethod paymentMethod;
    private String deliveryAddress;
    private String deliveryNotes;
    private BigDecimal deliveryLatitude;
    private BigDecimal deliveryLongitude;
    private BigDecimal calculatedDistanceKm;
    private LocalDateTime estimatedDeliveryTime;
    private LocalDateTime actualDeliveryTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Additional fields
    private List<OrderItemDTO> orderItems;
    private List<PaymentDTO> payments;
    private String formattedCreatedAt;
    private String formattedEstimatedDeliveryTime;
    private String formattedActualDeliveryTime;

    // Helper methods
    public String getOrderStatusDisplay() {
        return orderStatus != null ? getOrderStatusDisplayName(orderStatus) : "";
    }

    public String getPaymentStatusDisplay() {
        return paymentStatus != null ? getPaymentStatusDisplayName(paymentStatus) : "";
    }

    public String getPaymentMethodDisplay() {
        return paymentMethod != null ? getPaymentMethodDisplayName(paymentMethod) : "";
    }

    public boolean isDelivered() {
        return orderStatus == Order.OrderStatus.DONE;
    }

    public boolean isCancelled() {
        // Since we removed CANCELLED status, check if delivery notes contain cancellation note
        return deliveryNotes != null && deliveryNotes.contains("[ĐÃ HỦY]");
    }

    public boolean canBeCancelled() {
        return orderStatus == Order.OrderStatus.PENDING ||
                orderStatus == Order.OrderStatus.CONFIRMED;
    }

    // Helper methods for display names
    private String getOrderStatusDisplayName(Order.OrderStatus status) {
        if (status == null) return "UNKNOWN";
        
        switch (status) {
            case PENDING:
                return "Chờ xử lý";
            case CONFIRMED:
                return "Đã nhận";
            case DELIVERING:
                return "Đang giao";
            case DONE:
                return "Thanh toán thành công";
            default:
                return status.toString();
        }
    }

    private String getPaymentStatusDisplayName(Order.PaymentStatus status) {
        if (status == null) return "UNKNOWN";
        
        switch (status) {
            case PENDING:
                return "Chờ thanh toán";
            case COMPLETED:
                return "Đã thanh toán";
            case FAILED:
                return "Thanh toán thất bại";
            case REFUNDED:
                return "Đã hoàn tiền";
            default:
                return status.toString();
        }
    }

    private String getPaymentMethodDisplayName(Order.PaymentMethod method) {
        if (method == null) return "UNKNOWN";
        
        switch (method) {
            case CASH:
                return "Tiền mặt";
            case CARD:
                return "Thẻ";
            case BANK_TRANSFER:
                return "Chuyển khoản";
            case E_WALLET:
                return "Ví điện tử";
            default:
                return method.toString();
        }
    }
}
