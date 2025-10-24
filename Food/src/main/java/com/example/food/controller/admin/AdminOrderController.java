package com.example.food.controller.admin;

import com.example.food.dto.ApiResponse;
import com.example.food.dto.OrderDTO;
import com.example.food.dto.OrderStatsDTO;
import com.example.food.model.Order;
import com.example.food.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@Slf4j
public class AdminOrderController {

    private final OrderService orderService;

    // ===============================
    // WEB PAGES (Thymeleaf)
    // ===============================

    /**
     * Trang danh sách đơn hàng
     */
    @GetMapping
    public String ordersPage(Model model) {
        try {
            // Get all orders (you can add pagination later)
            List<OrderDTO> orders = orderService.getAllOrders();
            model.addAttribute("orders", orders);
            model.addAttribute("pageTitle", "Quản lý đơn hàng");
            return "admin/orders/list";
        } catch (Exception e) {
            log.error("Error loading orders page: ", e);
            model.addAttribute("error", "Lỗi tải danh sách đơn hàng: " + e.getMessage());
            return "admin/orders/list";
        }
    }

    /**
     * Trang chi tiết đơn hàng
     */
    @GetMapping("/{orderId}")
    public String orderDetailPage(@PathVariable Long orderId, Model model) {
        try {
            OrderDTO order = orderService.getOrderById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại"));

            model.addAttribute("order", order);
            model.addAttribute("pageTitle", "Chi tiết đơn hàng: " + order.getOrderNumber());
            return "admin/orders/detail";
        } catch (Exception e) {
            log.error("Error loading order detail page for ID {}: ", orderId, e);
            model.addAttribute("error", "Lỗi tải chi tiết đơn hàng: " + e.getMessage());
            return "redirect:/admin/orders";
        }
    }

    // ===============================
    // API ENDPOINTS (JSON)
    // ===============================

    /**
     * Lấy tất cả đơn hàng (API)
     */
    @GetMapping("/api/all")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getAllOrders() {
        try {
            List<OrderDTO> orders = orderService.getAllOrders();

            return ResponseEntity.ok(ApiResponse.<List<OrderDTO>>builder()
                    .success(true)
                    .message("Lấy danh sách đơn hàng thành công")
                    .data(orders)
                    .build());
        } catch (Exception e) {
            log.error("Error getting all orders: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<List<OrderDTO>>builder()
                    .success(false)
                    .message("Lỗi lấy danh sách đơn hàng: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Lấy đơn hàng theo trạng thái (API)
     */
    @GetMapping("/api/status/{status}")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getOrdersByStatus(@PathVariable Order.OrderStatus status) {
        try {
            List<OrderDTO> orders = orderService.getOrdersByStatus(status);

            return ResponseEntity.ok(ApiResponse.<List<OrderDTO>>builder()
                    .success(true)
                    .message("Lấy đơn hàng theo trạng thái thành công")
                    .data(orders)
                    .build());
        } catch (Exception e) {
            log.error("Error getting orders by status {}: ", status, e);
            return ResponseEntity.badRequest().body(ApiResponse.<List<OrderDTO>>builder()
                    .success(false)
                    .message("Lỗi lấy đơn hàng theo trạng thái: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Cập nhật trạng thái đơn hàng (API)
     */
    @PutMapping("/api/{orderId}/status")
    @ResponseBody
    public ResponseEntity<ApiResponse<OrderDTO>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status) {
        try {
            // Convert old status to new status
            Order.OrderStatus newStatus = convertToNewOrderStatus(status);
            OrderDTO order = orderService.updateOrderStatus(orderId, newStatus);

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
     * Convert old order status to new order status
     */
    private Order.OrderStatus convertToNewOrderStatus(String status) {
        switch (status.toUpperCase()) {
            case "PENDING":
                return Order.OrderStatus.PENDING;
            case "CONFIRMED":
                return Order.OrderStatus.CONFIRMED;
            case "PREPARING":
            case "READY":
            case "OUT_FOR_DELIVERY":
            case "DELIVERING":
                return Order.OrderStatus.DELIVERING;
            case "DELIVERED":
            case "COMPLETED":
            case "DONE":
                return Order.OrderStatus.DONE;
            case "CANCELLED":
                return Order.OrderStatus.DONE; // Cancelled orders are marked as DONE with note
            default:
                throw new IllegalArgumentException("Invalid order status: " + status);
        }
    }

    /**
     * Hủy đơn hàng (API)
     */
    @PutMapping("/api/{orderId}/cancel")
    @ResponseBody
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
     * Lấy thống kê đơn hàng (API)
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<ApiResponse<OrderStatsDTO>> getOrderStats() {
        try {
            OrderStatsDTO stats = orderService.getOrderStats();

            return ResponseEntity.ok(ApiResponse.<OrderStatsDTO>builder()
                    .success(true)
                    .message("Lấy thống kê đơn hàng thành công")
                    .data(stats)
                    .build());
        } catch (Exception e) {
            log.error("Error getting order stats: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<OrderStatsDTO>builder()
                    .success(false)
                    .message("Lỗi lấy thống kê đơn hàng: " + e.getMessage())
                    .build());
        }
    }
}
