package com.example.food.controller.admin;

import com.example.food.dto.ApiResponse;
import com.example.food.model.Notification;
import com.example.food.model.User;
import com.example.food.repository.UserRepository;
import com.example.food.security.JwtTokenProvider;
import com.example.food.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Lấy admin ID từ session hoặc SecurityContext
     */
    private Long getAdminIdFromSession(Model model, HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                String email = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null && user.isAdmin()) {
                    return user.getUserId();
                }
            }

            if (principal instanceof String) {
                String email = (String) principal;
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null && user.isAdmin()) {
                    return user.getUserId();
                }
            }
        }

        Object userId = request.getSession().getAttribute("adminUserId");
        if (userId instanceof Long) {
            return (Long) userId;
        }

        return userRepository.findByRoleIdAndIsActiveTrue(1)
                .stream()
                .findFirst()
                .map(User::getUserId)
                .orElse(1L);
    }

    /**
     * Trang danh sách notifications cho admin
     */
    @GetMapping("/notifications")
    public String notificationsPage(Model model, HttpServletRequest request) {
        try {
            Long adminId = getAdminIdFromSession(model, request);
            List<Notification> notifications = notificationService.getUserNotifications(adminId);
            Long unreadCount = notificationService.getUnreadCount(adminId);

            model.addAttribute("notifications", notifications);
            model.addAttribute("unreadCount", unreadCount);
            model.addAttribute("pageTitle", "Thông báo");
            return "admin/notifications";
        } catch (Exception e) {
            log.error("Error loading notifications page", e);
            model.addAttribute("error", "Lỗi tải thông báo: " + e.getMessage());
            return "admin/notifications";
        }
    }

    // ===============================
    // API ENDPOINTS
    // ===============================

    /**
     * API: Lấy tất cả notifications của admin
     */
    @GetMapping("/api/notifications")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications(
            Model model,
            HttpServletRequest request) {
        try {
            Long adminId = getAdminIdFromSession(model, request);
            List<Notification> notifications = notificationService.getUserNotifications(adminId);

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
     * API: Lấy notifications chưa đọc
     */
    @GetMapping("/api/notifications/unread")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<Notification>>> getUnreadNotifications(
            Model model,
            HttpServletRequest request) {
        try {
            Long adminId = getAdminIdFromSession(model, request);
            List<Notification> notifications = notificationService.getUnreadNotifications(adminId);

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
     * API: Đếm số notifications chưa đọc
     */
    @GetMapping("/api/notifications/unread/count")
    @ResponseBody
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            Model model,
            HttpServletRequest request) {
        try {
            Long adminId = getAdminIdFromSession(model, request);
            Long count = notificationService.getUnreadCount(adminId);

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
     * API: Đánh dấu notification là đã đọc
     */
    @PutMapping("/api/notifications/{notificationId}/read")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId,
            Model model,
            HttpServletRequest request) {
        try {
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
     * API: Đánh dấu tất cả notifications là đã đọc
     */
    @PutMapping("/api/notifications/read-all")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            Model model,
            HttpServletRequest request) {
        try {
            Long adminId = getAdminIdFromSession(model, request);
            notificationService.markAllAsRead(adminId);

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
