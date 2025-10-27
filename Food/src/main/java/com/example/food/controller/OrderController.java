package com.example.food.controller;

import com.example.food.dto.*;
import com.example.food.model.Order;
import com.example.food.service.OrderService;
import com.example.food.service.UserService;
import com.example.food.service.ShippingFeeSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;
    private final ShippingFeeSettingsService shippingFeeSettingsService;

    // ===============================
    // HELPER METHODS
    // ===============================

    /**
     * Lấy user hiện tại từ authentication context
     */
    private com.example.food.model.User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("Người dùng chưa đăng nhập");
        }

        String email = authentication.getName();
        return userService.getUserByEmail(email);
    }

    /**
     * Test endpoint để kiểm tra authentication
     */
    @GetMapping("/test-auth")
    public ResponseEntity<ApiResponse<String>> testAuth() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String message = "Authentication: " + authentication +
                    ", Principal: " + (authentication != null ? authentication.getPrincipal() : "null") +
                    ", Authenticated: " + (authentication != null ? authentication.isAuthenticated() : "null");

            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true)
                    .message("Test authentication")
                    .data(message)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Test endpoint để tạo order với userId cụ thể (tạm thời để test)
     */
    @PostMapping("/test-create")
    public ResponseEntity<ApiResponse<OrderDTO>> testCreateOrder(@RequestBody CreateOrderRequest request) {
        try {
            log.info("Test create order - UserId from request: {}", request.getUserId());

            // Nếu không có userId trong request, sử dụng user mặc định
            if (request.getUserId() == null) {
                // Tìm user đầu tiên trong database để test
                com.example.food.model.User defaultUser = userService.getUserByEmail("test@example.com");
                if (defaultUser == null) {
                    // Nếu không có user test, tạo một user tạm thời
                    defaultUser = com.example.food.model.User.builder()
                            .email("test@example.com")
                            .fullName("Test User")
                            .phoneNumber("0123456789")
                            .address("Test Address")
                            .password("password")
                            .build();
                    defaultUser = userService.saveUser(defaultUser);
                } else {
                    // Đảm bảo user có địa chỉ
                    if (defaultUser.getAddress() == null || defaultUser.getAddress().trim().isEmpty()) {
                        defaultUser.setAddress("Test Address");
                        defaultUser = userService.saveUser(defaultUser);
                        log.info("Updated user address for test: {}", defaultUser.getUserId());
                    }
                }
                request.setUserId(defaultUser.getUserId());
                log.info("Using default user for test: {} with address: {}", defaultUser.getUserId(), defaultUser.getAddress());
            }

            OrderDTO order = orderService.createOrder(request);

            return ResponseEntity.ok(ApiResponse.<OrderDTO>builder()
                    .success(true)
                    .message("Test order created successfully")
                    .data(order)
                    .build());
        } catch (Exception e) {
            log.error("Error creating test order: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<OrderDTO>builder()
                    .success(false)
                    .message("Error creating test order: " + e.getMessage())
                    .build());
        }
    }

    // ===============================
    // ORDER MANAGEMENT APIs
    // ===============================

    /**
     * Tạo đơn hàng mới
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderDTO>> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            log.debug("OrderController - createOrder called");

            // Debug authentication context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            log.debug("OrderController - Authentication: {}, Principal: {}, Authenticated: {}",
                    authentication,
                    authentication != null ? authentication.getPrincipal() : "null",
                    authentication != null ? authentication.isAuthenticated() : "null");

            // Lấy user hiện tại từ authentication context
            com.example.food.model.User currentUser = getCurrentUser();

            // Set userId vào request
            request.setUserId(currentUser.getUserId());

            log.info("Creating order for user: {} (email: {})", currentUser.getUserId(), currentUser.getEmail());

            OrderDTO order = orderService.createOrder(request);

            return ResponseEntity.ok(ApiResponse.<OrderDTO>builder()
                    .success(true)
                    .message("Đơn hàng đã được tạo thành công")
                    .data(order)
                    .build());
        } catch (Exception e) {
            log.error("Error creating order: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<OrderDTO>builder()
                    .success(false)
                    .message("Lỗi tạo đơn hàng: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Lấy danh sách đơn hàng của user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getOrdersByUserId(@PathVariable Long userId) {
        try {
            List<OrderDTO> orders = orderService.getOrdersByUserId(userId);

            return ResponseEntity.ok(ApiResponse.<List<OrderDTO>>builder()
                    .success(true)
                    .message("Lấy danh sách đơn hàng thành công")
                    .data(orders)
                    .build());
        } catch (Exception e) {
            log.error("Error getting orders for user {}: ", userId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<List<OrderDTO>>builder()
                    .success(false)
                    .message("Lỗi lấy danh sách đơn hàng: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Lấy chi tiết đơn hàng theo ID
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderById(@PathVariable Long orderId) {
        try {
            OrderDTO order = orderService.getOrderById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại"));

            return ResponseEntity.ok(ApiResponse.<OrderDTO>builder()
                    .success(true)
                    .message("Lấy thông tin đơn hàng thành công")
                    .data(order)
                    .build());
        } catch (Exception e) {
            log.error("Error getting order {}: ", orderId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<OrderDTO>builder()
                    .success(false)
                    .message("Lỗi lấy thông tin đơn hàng: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Lấy đơn hàng theo order number
     */
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderByNumber(@PathVariable String orderNumber) {
        try {
            OrderDTO order = orderService.getOrderByOrderNumber(orderNumber)
                    .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại"));

            return ResponseEntity.ok(ApiResponse.<OrderDTO>builder()
                    .success(true)
                    .message("Lấy thông tin đơn hàng thành công")
                    .data(order)
                    .build());
        } catch (Exception e) {
            log.error("Error getting order by number {}: ", orderNumber, e);
            return ResponseEntity.badRequest().body(ApiResponse.<OrderDTO>builder()
                    .success(false)
                    .message("Lỗi lấy thông tin đơn hàng: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Cập nhật trạng thái đơn hàng
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderDTO>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam Order.OrderStatus status) {
        try {
            OrderDTO order = orderService.updateOrderStatus(orderId, status);

            return ResponseEntity.ok(ApiResponse.<OrderDTO>builder()
                    .success(true)
                    .message("Cập nhật trạng thái đơn hàng thành công")
                    .data(order)
                    .build());
        } catch (Exception e) {
            log.error("Error updating order status for {}: ", orderId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<OrderDTO>builder()
                    .success(false)
                    .message("Lỗi cập nhật trạng thái đơn hàng: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Hủy đơn hàng
     */
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderDTO>> cancelOrder(@PathVariable Long orderId) {
        try {
            OrderDTO order = orderService.cancelOrder(orderId);

            return ResponseEntity.ok(ApiResponse.<OrderDTO>builder()
                    .success(true)
                    .message("Hủy đơn hàng thành công")
                    .data(order)
                    .build());
        } catch (Exception e) {
            log.error("Error cancelling order {}: ", orderId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<OrderDTO>builder()
                    .success(false)
                    .message("Lỗi hủy đơn hàng: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Lấy thông tin phí ship từ database
     */
    @GetMapping("/shipping-fee")
    public ResponseEntity<ApiResponse<ShippingFeeInfo>> getShippingFeeInfo() {
        try {
            log.info("Getting shipping fee info");

            // Lấy cài đặt phí ship hiện tại
            Optional<ShippingFeeSettingsDTO> settings = shippingFeeSettingsService.getCurrentSettings();

            ShippingFeeInfo info = new ShippingFeeInfo();
            if (settings.isPresent()) {
                ShippingFeeSettingsDTO dto = settings.get();
                info.setDefaultShippingFee(dto.getFixedShippingFee());
                info.setFreeShippingThreshold(dto.getFreeShippingThreshold());
                info.setMinOrderAmount(BigDecimal.valueOf(50000)); // Default min order amount
            } else {
                // Fallback to 0 when no settings
                info.setDefaultShippingFee(BigDecimal.ZERO);
                info.setFreeShippingThreshold(BigDecimal.ZERO);
                info.setMinOrderAmount(BigDecimal.valueOf(50000));
            }

            return ResponseEntity.ok(ApiResponse.<ShippingFeeInfo>builder()
                    .success(true)
                    .message("Lấy thông tin phí ship thành công")
                    .data(info)
                    .build());

        } catch (Exception e) {
            log.error("Error getting shipping fee info: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.<ShippingFeeInfo>builder()
                    .success(false)
                    .message("Lỗi khi lấy thông tin phí ship: " + e.getMessage())
                    .build());
        }
    }

}
