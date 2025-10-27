package com.example.food.controller.admin;

import com.example.food.dto.ApiResponse;
import com.example.food.dto.UserDTO;
import com.example.food.model.User;
import com.example.food.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {

    private final UserService userService;

    // ===============================
    // WEB PAGES (Thymeleaf)
    // ===============================

    /**
     * Trang danh sách người dùng
     */
    @GetMapping
    public String usersPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            Model model) {

        try {
            // TODO: Implement user search and pagination in UserService
            List<User> users = userService.getAllUsers(); // Temporary - need to implement pagination

            model.addAttribute("users", users);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", 1); // Temporary
            model.addAttribute("totalElements", users.size());
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);
            model.addAttribute("search", search);
            model.addAttribute("pageTitle", "Người dùng - Danh sách");

            return "admin/users/list";
        } catch (Exception e) {
            log.error("Error loading users page: ", e);
            model.addAttribute("error", "Lỗi tải danh sách người dùng: " + e.getMessage());
            return "admin/users/list";
        }
    }

    /**
     * Trang chi tiết người dùng
     */
    @GetMapping("/view/{userId}")
    public String viewUserPage(@PathVariable Long userId, Model model) {
        try {
            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

            model.addAttribute("user", user);
            model.addAttribute("pageTitle", "Người dùng - Chi tiết");

            return "admin/users/view";
        } catch (Exception e) {
            log.error("Error loading view user page for ID {}: ", userId, e);
            model.addAttribute("error", "Lỗi tải trang chi tiết: " + e.getMessage());
            return "redirect:/admin/users";
        }
    }

    // ===============================
    // API ENDPOINTS (JSON)
    // ===============================

    /**
     * Lấy danh sách người dùng (API)
     */
    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<UserDTO>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<User> users = userService.getAllUsers();
            List<UserDTO> userDTOs = users.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.<List<UserDTO>>builder()
                    .success(true)
                    .message("Lấy danh sách người dùng thành công")
                    .data(userDTOs)
                    .build());
        } catch (Exception e) {
            log.error("Error getting users: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<List<UserDTO>>builder()
                    .success(false)
                    .message("Lỗi lấy danh sách người dùng: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Lấy thông tin người dùng theo ID (API)
     */
    @GetMapping("/api/{userId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable Long userId) {
        try {
            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

            return ResponseEntity.ok(ApiResponse.<UserDTO>builder()
                    .success(true)
                    .message("Lấy thông tin người dùng thành công")
                    .data(convertToDTO(user))
                    .build());
        } catch (Exception e) {
            log.error("Error getting user {}: ", userId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<UserDTO>builder()
                    .success(false)
                    .message("Lỗi lấy thông tin người dùng: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Khóa/Mở khóa tài khoản người dùng (API)
     */
    @PutMapping("/api/toggle-status/{userId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<UserDTO>> toggleUserStatus(@PathVariable Long userId) {
        try {
            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

            // TODO: Implement toggle status in UserService
            // user.setIsActive(!user.getIsActive());
            // User updatedUser = userService.updateUser(user);

            return ResponseEntity.ok(ApiResponse.<UserDTO>builder()
                    .success(true)
                    .message("Cập nhật trạng thái người dùng thành công")
                    .data(convertToDTO(user))
                    .build());
        } catch (Exception e) {
            log.error("Error toggling user status {}: ", userId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<UserDTO>builder()
                    .success(false)
                    .message("Lỗi cập nhật trạng thái: " + e.getMessage())
                    .build());
        }
    }

    // ===============================
    // HELPER METHODS
    // ===============================

    private UserDTO convertToDTO(User user) {
        return UserDTO.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .avatarUrl(user.getAvatarUrl())
                .roleId(user.getRoleId())
                .build();
    }
}
