package com.example.food.controller.admin;

import com.example.food.model.Category;
import com.example.food.model.Product;
import com.example.food.repository.UserRepository;
import com.example.food.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final ProductService productService;

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
