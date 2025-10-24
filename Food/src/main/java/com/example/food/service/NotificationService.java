package com.example.food.service;

import com.example.food.dto.OrderDTO;
import com.example.food.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Gửi thông báo order mới cho admin
     */
    public void notifyNewOrder(OrderDTO order) {
        try {
            // Tạo thông báo
            OrderNotification notification = OrderNotification.builder()
                    .type("NEW_ORDER")
                    .orderId(order.getOrderId())
                    .orderNumber(order.getOrderNumber())
                    .customerName(order.getUserFullName())
                    .totalAmount(order.getFinalAmount().toString())
                    .orderStatus(getOrderStatusDisplayName(order.getOrderStatus()))
                    .message("Có đơn hàng mới từ " + order.getUserFullName())
                    .timestamp(System.currentTimeMillis())
                    .build();

            // Gửi đến topic "/topic/admin/orders"
            messagingTemplate.convertAndSend("/topic/admin/orders", notification);

            log.info("Sent new order notification: {}", order.getOrderNumber());

        } catch (Exception e) {
            log.error("Error sending order notification: ", e);
        }
    }

    /**
     * Gửi thông báo cập nhật trạng thái order
     */
    public void notifyOrderStatusUpdate(Long orderId, String orderNumber, String newStatus) {
        try {
            OrderNotification notification = OrderNotification.builder()
                    .type("ORDER_STATUS_UPDATE")
                    .orderId(orderId)
                    .orderNumber(orderNumber)
                    .orderStatus(newStatus)
                    .message("Đơn hàng " + orderNumber + " đã được cập nhật thành " + newStatus)
                    .timestamp(System.currentTimeMillis())
                    .build();

            // Gửi đến topic "/topic/admin/orders"
            messagingTemplate.convertAndSend("/topic/admin/orders", notification);

            log.info("Sent order status update notification: {}", orderNumber);

        } catch (Exception e) {
            log.error("Error sending order status update notification: ", e);
        }
    }

    /**
     * Class để chứa thông tin thông báo
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class OrderNotification {
        private String type;
        private Long orderId;
        private String orderNumber;
        private String customerName;
        private String totalAmount;
        private String orderStatus;
        private String message;
        private Long timestamp;
    }

    /**
     * Helper method để lấy tên hiển thị của OrderStatus
     */
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
}
