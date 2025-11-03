package com.example.food.controller;

import com.example.food.dto.ApiResponse;
import com.example.food.model.Notification;
import com.example.food.model.User;
import com.example.food.repository.UserRepository;
import com.example.food.security.JwtTokenProvider;
import com.example.food.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Lấy user ID từ JWT token
     */
    private Long getUserIdFromToken(String token) {
        String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getUserId();
    }

    /**
     * Lấy tất cả notifications của user
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications(
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = getUserIdFromToken(token);
            List<Notification> notifications = notificationService.getUserNotifications(userId);

            return ResponseEntity.ok(ApiResponse.<List<Notification>>builder()
                    .success(true)
                    .message("Notifications retrieved")
                    .data(notifications)
                    .build());
        } catch (Exception e) {
            log.error("Error getting notifications", e);
            return ResponseEntity.badRequest().body(ApiResponse.<List<Notification>>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * Lấy notifications chưa đọc của user
     */
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<Notification>>> getUnreadNotifications(
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = getUserIdFromToken(token);
            List<Notification> notifications = notificationService.getUnreadNotifications(userId);

            return ResponseEntity.ok(ApiResponse.<List<Notification>>builder()
                    .success(true)
                    .message("Unread notifications retrieved")
                    .data(notifications)
                    .build());
        } catch (Exception e) {
            log.error("Error getting unread notifications", e);
            return ResponseEntity.badRequest().body(ApiResponse.<List<Notification>>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * Đếm số notifications chưa đọc của user
     */
    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = getUserIdFromToken(token);
            Long count = notificationService.getUnreadCount(userId);

            return ResponseEntity.ok(ApiResponse.<Long>builder()
                    .success(true)
                    .message("Unread count retrieved")
                    .data(count)
                    .build());
        } catch (Exception e) {
            log.error("Error getting unread count", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Long>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * Đánh dấu notification là đã đọc
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @RequestHeader("Authorization") String token,
            @PathVariable Long notificationId) {
        try {
            Long userId = getUserIdFromToken(token);
            // TODO: Verify notification belongs to user
            notificationService.markAsRead(notificationId);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Notification marked as read")
                    .build());
        } catch (Exception e) {
            log.error("Error marking notification as read", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Void>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * Đánh dấu tất cả notifications là đã đọc
     */
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = getUserIdFromToken(token);
            notificationService.markAllAsRead(userId);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("All notifications marked as read")
                    .build());
        } catch (Exception e) {
            log.error("Error marking all notifications as read", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Void>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
}
