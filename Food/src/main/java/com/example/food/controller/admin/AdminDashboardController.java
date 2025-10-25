package com.example.food.controller.admin;

import com.example.food.dto.ApiResponse;
import com.example.food.model.Category;
import com.example.food.model.Product;
import com.example.food.repository.UserRepository;
import com.example.food.service.ProductService;
import com.example.food.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final ProductService productService;
    private final OrderService orderService;

    /**
     * Dashboard chính
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        log.info("Accessing admin dashboard");

        try {
            // Thống kê tổng quan
            long totalUsers = userRepository.count();

            // Xử lý products với try-catch riêng
            long totalProducts = 0;
            try {
                totalProducts = productService.getAllAvailableProducts(org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();
            } catch (Exception e) {
                log.warn("Error getting total products: {}", e.getMessage());
            }

            long totalCategories = 0;
            try {
                totalCategories = productService.getAllActiveCategories().size();
            } catch (Exception e) {
                log.warn("Error getting total categories: {}", e.getMessage());
            }

            // Sản phẩm nổi bật
            List<Product> featuredProducts = new java.util.ArrayList<>();
            try {
                featuredProducts = productService.getFeaturedProducts();
            } catch (Exception e) {
                log.warn("Error getting featured products: {}", e.getMessage());
            }

            // Danh mục có sản phẩm
            List<Category> categoriesWithProducts = new java.util.ArrayList<>();
            try {
                categoriesWithProducts = productService.getCategoriesWithProducts();
            } catch (Exception e) {
                log.warn("Error getting categories with products: {}", e.getMessage());
            }

            // Sản phẩm có nhiều ảnh
            List<Product> productsWithGallery = new java.util.ArrayList<>();
            try {
                productsWithGallery = productService.getProductsWithGallery();
            } catch (Exception e) {
                log.warn("Error getting products with gallery: {}", e.getMessage());
            }

            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("totalCategories", totalCategories);
            model.addAttribute("featuredProducts", featuredProducts);
            model.addAttribute("categoriesWithProducts", categoriesWithProducts);
            model.addAttribute("productsWithGallery", productsWithGallery);
            model.addAttribute("pageTitle", "Admin Dashboard");

            return "admin/dashboard";
        } catch (Exception e) {
            log.error("Error loading admin dashboard: ", e);
            model.addAttribute("error", "Lỗi tải dashboard: " + e.getMessage());
            model.addAttribute("totalUsers", 0);
            model.addAttribute("totalProducts", 0);
            model.addAttribute("totalCategories", 0);
            model.addAttribute("featuredProducts", new java.util.ArrayList<>());
            model.addAttribute("categoriesWithProducts", new java.util.ArrayList<>());
            model.addAttribute("productsWithGallery", new java.util.ArrayList<>());
            model.addAttribute("pageTitle", "Admin Dashboard");
            return "admin/dashboard";
        }
    }

    // ===============================
    // API ENDPOINTS FOR CHARTS
    // ===============================

    /**
     * API: Lấy dữ liệu order theo ngày/tuần/tháng
     */
    @GetMapping("/api/dashboard/orders")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderChartData(
            @RequestParam(defaultValue = "day") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            Map<String, Object> chartData = new HashMap<>();
            
            // Set default date range if not provided
            LocalDate defaultStartDate = startDate != null ? startDate : LocalDate.now().minusDays(30);
            LocalDate defaultEndDate = endDate != null ? endDate : LocalDate.now();
            
            switch (period.toLowerCase()) {
                case "day":
                    chartData = getOrderDataByDay(defaultStartDate, defaultEndDate);
                    break;
                case "week":
                    chartData = getOrderDataByWeek(defaultStartDate, defaultEndDate);
                    break;
                case "month":
                    chartData = getOrderDataByMonth(defaultStartDate, defaultEndDate);
                    break;
                default:
                    chartData = getOrderDataByDay(defaultStartDate, defaultEndDate);
            }
            
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Lấy dữ liệu biểu đồ thành công")
                    .data(chartData)
                    .build());
                    
        } catch (Exception e) {
            log.error("Error getting order chart data: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Lỗi lấy dữ liệu biểu đồ: " + e.getMessage())
                    .build());
        }
    }

    /**
     * API: Lấy thống kê tổng quan
     */
    @GetMapping("/api/dashboard/stats")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Total counts
            long totalUsers = userRepository.count();
            long totalProducts = productService.getAllAvailableProducts(org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();
            long totalCategories = productService.getAllActiveCategories().size();
            
            // Order stats (last 30 days)
            LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
            long totalOrders = orderService.getOrderCountByDateRange(thirtyDaysAgo, LocalDate.now());
            double totalRevenue = orderService.getRevenueByDateRange(thirtyDaysAgo, LocalDate.now());
            
            // Today's stats
            LocalDate today = LocalDate.now();
            long todayOrders = orderService.getOrderCountByDateRange(today, today);
            double todayRevenue = orderService.getRevenueByDateRange(today, today);
            
            stats.put("totalUsers", totalUsers);
            stats.put("totalProducts", totalProducts);
            stats.put("totalCategories", totalCategories);
            stats.put("totalOrders", totalOrders);
            stats.put("totalRevenue", totalRevenue);
            stats.put("todayOrders", todayOrders);
            stats.put("todayRevenue", todayRevenue);
            
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Lấy thống kê thành công")
                    .data(stats)
                    .build());
                    
        } catch (Exception e) {
            log.error("Error getting dashboard stats: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Lỗi lấy thống kê: " + e.getMessage())
                    .build());
        }
    }

    // ===============================
    // PRIVATE HELPER METHODS
    // ===============================

    private Map<String, Object> getOrderDataByDay(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> data = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<Long> orderCounts = new ArrayList<>();
        List<Double> revenues = new ArrayList<>();
        
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            labels.add(current.format(DateTimeFormatter.ofPattern("dd/MM")));
            
            long count = orderService.getOrderCountByDateRange(current, current);
            double revenue = orderService.getRevenueByDateRange(current, current);
            
            orderCounts.add(count);
            revenues.add(revenue);
            
            current = current.plusDays(1);
        }
        
        data.put("labels", labels);
        data.put("orderCounts", orderCounts);
        data.put("revenues", revenues);
        data.put("period", "day");
        
        return data;
    }

    private Map<String, Object> getOrderDataByWeek(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> data = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<Long> orderCounts = new ArrayList<>();
        List<Double> revenues = new ArrayList<>();
        
        LocalDate current = startDate.with(java.time.DayOfWeek.MONDAY);
        while (!current.isAfter(endDate)) {
            LocalDate weekEnd = current.plusDays(6);
            if (weekEnd.isAfter(endDate)) {
                weekEnd = endDate;
            }
            
            labels.add("Tuần " + current.format(DateTimeFormatter.ofPattern("dd/MM")));
            
            long count = orderService.getOrderCountByDateRange(current, weekEnd);
            double revenue = orderService.getRevenueByDateRange(current, weekEnd);
            
            orderCounts.add(count);
            revenues.add(revenue);
            
            current = current.plusWeeks(1);
        }
        
        data.put("labels", labels);
        data.put("orderCounts", orderCounts);
        data.put("revenues", revenues);
        data.put("period", "week");
        
        return data;
    }

    private Map<String, Object> getOrderDataByMonth(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> data = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<Long> orderCounts = new ArrayList<>();
        List<Double> revenues = new ArrayList<>();
        
        LocalDate current = startDate.withDayOfMonth(1);
        while (!current.isAfter(endDate)) {
            LocalDate monthEnd = current.withDayOfMonth(current.lengthOfMonth());
            if (monthEnd.isAfter(endDate)) {
                monthEnd = endDate;
            }
            
            labels.add(current.format(DateTimeFormatter.ofPattern("MM/yyyy")));
            
            long count = orderService.getOrderCountByDateRange(current, monthEnd);
            double revenue = orderService.getRevenueByDateRange(current, monthEnd);
            
            orderCounts.add(count);
            revenues.add(revenue);
            
            current = current.plusMonths(1);
        }
        
        data.put("labels", labels);
        data.put("orderCounts", orderCounts);
        data.put("revenues", revenues);
        data.put("period", "month");
        
        return data;
    }

    /**
     * Trang đăng nhập admin
     */
    @GetMapping("/login")
    public String login() {
        return "redirect:/admin/auth/login";
    }

    /**
     * Trang chủ admin
     */
    @GetMapping
    public String adminHome() {
        return "redirect:/admin/dashboard";
    }
}