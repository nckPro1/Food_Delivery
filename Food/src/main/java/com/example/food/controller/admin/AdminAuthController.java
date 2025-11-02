package com.example.food.controller.admin;

import com.example.food.model.User;
import com.example.food.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
@Slf4j
public class AdminAuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    // ===============================
    // WEB PAGES (Thymeleaf)
    // ===============================

    /**
     * Trang đăng nhập admin
     */
    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Email hoặc mật khẩu không đúng!");
        }
        model.addAttribute("pageTitle", "Đăng nhập Admin");
        return "admin/auth/login";
    }

    /**
     * Trang test
     */
    @GetMapping("/test")
    public String testPage() {
        log.info("Accessing test page");
        return "admin/auth/test";
    }

    /**
     * Trang đăng ký admin
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        log.info("Accessing admin register page");
        model.addAttribute("pageTitle", "Đăng ký Admin");
        return "admin/auth/register";
    }

    // Spring Security sẽ xử lý POST /admin/auth/login tự động

    /**
     * Xử lý đăng ký admin
     */
    @PostMapping("/register")
    public String register(@RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           @RequestParam String fullName,
                           @RequestParam(required = false) String phoneNumber,
                           Model model) {
        try {
            // Validation
            if (!password.equals(confirmPassword)) {
                model.addAttribute("error", "Mật khẩu xác nhận không khớp!");
                return "admin/auth/register";
            }

            if (password.length() < 6) {
                model.addAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự!");
                return "admin/auth/register";
            }

            // Kiểm tra email đã tồn tại
            try {
                userService.getUserByEmail(email);
                model.addAttribute("error", "Email đã được sử dụng!");
                return "admin/auth/register";
            } catch (Exception e) {
                // Email chưa tồn tại, tiếp tục tạo user
            }

            // Tạo user admin mới
            User adminUser = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .fullName(fullName)
                    .phoneNumber(phoneNumber)
                    .roleId(1) // Admin role = 1, User role = 2 (theo database)
                    .build();

            userService.saveUser(adminUser);

            log.info("Admin mới đã được tạo: {}", email);
            model.addAttribute("success", "Đăng ký admin thành công! Bạn có thể đăng nhập ngay.");
            return "admin/auth/login";

        } catch (Exception e) {
            log.error("Lỗi đăng ký admin: {}", e.getMessage());
            model.addAttribute("error", "Có lỗi xảy ra khi đăng ký: " + e.getMessage());
            return "admin/auth/register";
        }
    }

    /**
     * Đăng xuất admin
     */
    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        request.getSession().invalidate();
        log.info("Admin đã đăng xuất");
        return "redirect:/admin/auth/login";
    }

    /**
     * Endpoint tạm thời để fix roleId của admin
     * TODO: Xóa endpoint này sau khi đã fix xong
     */
    @GetMapping("/fix-admin-role")
    public String fixAdminRole(@RequestParam String email, Model model) {
        try {
            User user = userService.getUserByEmail(email);
            if (user != null) {
                user.setRoleId(1); // Set role = ADMIN (Admin = 1, User = 2 - theo database)
                userService.saveUser(user);
                log.info("Đã cập nhật roleId của user {} thành ADMIN (roleId=1)", email);
                model.addAttribute("success", "Đã cập nhật roleId thành công!");
            } else {
                model.addAttribute("error", "Không tìm thấy user với email: " + email);
            }
        } catch (Exception e) {
            log.error("Lỗi khi fix admin role: {}", e.getMessage());
            model.addAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "admin/auth/login";
    }
}
