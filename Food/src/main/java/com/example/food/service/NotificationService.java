package com.example.food.service;

import com.example.food.dto.OrderDTO;
import com.example.food.model.Notification;
import com.example.food.model.Order;
import com.example.food.model.User;
import com.example.food.repository.NotificationRepository;
import com.example.food.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // ===============================
    // ORDER NOTIFICATIONS
    // ===============================

    /**
     * Gửi thông báo order mới cho tất cả admin
     */
    @Transactional
    public void notifyNewOrder(OrderDTO order) {
        try {
            String message = "Có đơn hàng mới từ " + order.getUserFullName() + " - " + order.getOrderNumber();
            String title = "Đơn hàng mới";

            // Lấy tất cả admin
            List<User> admins = userRepository.findByRoleIdAndIsActiveTrue(1); // role_id = 1 là admin

            for (User admin : admins) {
                // Tạo notification trong database
                Notification notification = Notification.builder()
                        .userId(admin.getUserId())
                        .type(Notification.NotificationType.NEW_ORDER)
                        .title(title)
                        .message(message)
                        .isRead(false)
                        .relatedId(order.getOrderId())
                        .relatedType("ORDER")
                        .build();

                notification = notificationRepository.save(notification);

                // Gửi qua WebSocket
                sendNotificationToUser(admin.getUserId(), notification);
            }

            log.info("Sent new order notification to {} admins for order: {}", admins.size(), order.getOrderNumber());

        } catch (Exception e) {
            log.error("Error sending new order notification: ", e);
        }
    }

    /**
     * Gửi thông báo cập nhật trạng thái order cho user
     */
    @Transactional
    public void notifyOrderStatusUpdate(Long userId, Long orderId, String orderNumber, Order.OrderStatus newStatus) {
        try {
            String statusName = getOrderStatusDisplayName(newStatus);
            String title = "Cập nhật đơn hàng";
            String message = "Đơn hàng " + orderNumber + " đã được cập nhật thành: " + statusName;

            // Tạo notification cho user
            Notification notification = Notification.builder()
                    .userId(userId)
                    .type(Notification.NotificationType.ORDER_STATUS_UPDATED)
                    .title(title)
                    .message(message)
                    .isRead(false)
                    .relatedId(orderId)
                    .relatedType("ORDER")
                    .build();

            notification = notificationRepository.save(notification);

            // Gửi qua WebSocket
            sendNotificationToUser(userId, notification);

            log.info("Sent order status update notification to user {} for order: {}", userId, orderNumber);

        } catch (Exception e) {
            log.error("Error sending order status update notification: ", e);
        }
    }

    // ===============================
    // CHAT MESSAGE NOTIFICATIONS
    // ===============================

    /**
     * Gửi thông báo khi admin nhắn tin đến user
     */
    @Transactional
    public void notifyAdminMessage(Long userId, Long conversationId, String adminName, String messageContent) {
        try {
            String title = "Tin nhắn mới từ admin";
            String message = adminName + ": " + (messageContent.length() > 50
                    ? messageContent.substring(0, 50) + "..."
                    : messageContent);

            // Tạo notification cho user
            Notification notification = Notification.builder()
                    .userId(userId)
                    .type(Notification.NotificationType.ADMIN_MESSAGE)
                    .title(title)
                    .message(message)
                    .isRead(false)
                    .relatedId(conversationId)
                    .relatedType("CONVERSATION")
                    .build();

            notification = notificationRepository.save(notification);

            // Gửi qua WebSocket
            sendNotificationToUser(userId, notification);

            log.info("Sent admin message notification to user {} for conversation: {}", userId, conversationId);

        } catch (Exception e) {
            log.error("Error sending admin message notification: ", e);
        }
    }

    /**
     * Gửi thông báo khi user nhắn tin đến admin
     */
    @Transactional
    public void notifyUserMessage(Long conversationId, Long senderUserId, String userName, String messageContent) {
        try {
            String title = "Tin nhắn mới từ khách hàng";
            String message = userName + ": " + (messageContent.length() > 50
                    ? messageContent.substring(0, 50) + "..."
                    : messageContent);

            // Lấy tất cả admin
            List<User> admins = userRepository.findByRoleIdAndIsActiveTrue(1); // role_id = 1 là admin

            for (User admin : admins) {
                // Tạo notification cho mỗi admin
                Notification notification = Notification.builder()
                        .userId(admin.getUserId())
                        .type(Notification.NotificationType.USER_MESSAGE)
                        .title(title)
                        .message(message)
                        .isRead(false)
                        .relatedId(conversationId)
                        .relatedType("CONVERSATION")
                        .build();

                notification = notificationRepository.save(notification);

                // Gửi qua WebSocket
                sendNotificationToUser(admin.getUserId(), notification);
            }

            log.info("Sent user message notification to {} admins for conversation: {}", admins.size(), conversationId);

        } catch (Exception e) {
            log.error("Error sending user message notification: ", e);
        }
    }

    // ===============================
    // NOTIFICATION RETRIEVAL
    // ===============================

    /**
     * Lấy tất cả notifications của user
     */
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Lấy notifications chưa đọc của user
     */
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    /**
     * Đếm số notifications chưa đọc của user
     */
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Đánh dấu notification là đã đọc
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.markAsRead(notificationId);
    }

    /**
     * Đánh dấu tất cả notifications của user là đã đọc
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    // ===============================
    // WEBSOCKET HELPERS
    // ===============================

    /**
     * Gửi notification qua WebSocket đến user
     */
    private void sendNotificationToUser(Long userId, Notification notification) {
        try {
            // Gửi đến topic "/topic/user/{userId}/notifications"
            messagingTemplate.convertAndSend("/topic/user/" + userId + "/notifications", notification);

            // Nếu là admin, cũng gửi đến topic chung của admin
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.isAdmin()) {
                messagingTemplate.convertAndSend("/topic/admin/notifications", notification);
            }

        } catch (Exception e) {
            log.error("Error sending notification via WebSocket: ", e);
        }
    }

    // ===============================
    // HELPER METHODS
    // ===============================

    /**
     * Helper method để lấy tên hiển thị của OrderStatus
     */
    private String getOrderStatusDisplayName(Order.OrderStatus status) {
        if (status == null) return "UNKNOWN";

        switch (status) {
            case PENDING:
                return "Chờ xử lý";
            case CONFIRMED:
                return "Đã xác nhận";
            case DELIVERING:
                return "Đang giao hàng";
            case DONE:
                return "Hoàn thành";
            case CANCELLED:
                return "Đã hủy";
            default:
                return status.toString();
        }
    }

    // ===============================
    // LEGACY METHODS (for backward compatibility)
    // ===============================

    /**
     * Legacy method - giữ lại để tương thích
     */
    @Deprecated
    public void notifyOrderStatusUpdate(Long orderId, String orderNumber, String newStatus) {
        // This method is deprecated, use the new one with userId
        log.warn("Using deprecated notifyOrderStatusUpdate method");
    }
}
