package com.example.food.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "user_id", nullable = false)
    private Long userId; // Người nhận thông báo

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    // Dữ liệu bổ sung cho notification (JSON hoặc các trường riêng)
    @Column(name = "related_id") // ID của order, conversation, etc.
    private Long relatedId;

    @Column(name = "related_type") // "ORDER", "CONVERSATION", etc.
    private String relatedType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum NotificationType {
        // User notifications
        ADMIN_MESSAGE,           // Admin nhắn tin đến user
        ORDER_STATUS_UPDATED,    // Trạng thái đơn hàng được cập nhật

        // Admin notifications
        USER_MESSAGE,            // User nhắn tin đến admin
        NEW_ORDER                // Có đơn hàng mới
    }
}
