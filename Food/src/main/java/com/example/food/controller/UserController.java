package com.example.food.controller;

import com.example.food.dto.UserDTO;
import com.example.food.model.User;
import com.example.food.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Get current user profile
     */
    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getUserProfile(@AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails) {
        try {
            User user = userService.getUserByEmail(userDetails.getUsername());
            UserDTO userDTO = userService.convertToDTO(user);
            return ResponseEntity.ok(userDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update user profile
     */
    @PutMapping("/profile")
    public ResponseEntity<UserDTO> updateUserProfile(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails,
            @RequestBody UserDTO userDTO) {
        try {
            User user = userService.getUserByEmail(userDetails.getUsername());

            // Update user fields
            if (userDTO.getFullName() != null) {
                user.setFullName(userDTO.getFullName());
            }
            if (userDTO.getPhoneNumber() != null) {
                user.setPhoneNumber(userDTO.getPhoneNumber());
            }
            if (userDTO.getAddress() != null) {
                user.setAddress(userDTO.getAddress());
            }
            if (userDTO.getAvatarUrl() != null) {
                user.setAvatarUrl(userDTO.getAvatarUrl());
            }

            // Save updated user
            User updatedUser = userService.saveUser(user);
            UserDTO updatedUserDTO = userService.convertToDTO(updatedUser);

            return ResponseEntity.ok(updatedUserDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Upload user avatar
     */
    @PostMapping("/upload-avatar")
    public ResponseEntity<com.example.food.dto.ApiResponse> uploadAvatar(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        com.example.food.dto.ApiResponse.builder()
                                .success(false)
                                .message("File không được để trống")
                                .build()
                );
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(
                        com.example.food.dto.ApiResponse.builder()
                                .success(false)
                                .message("Chỉ chấp nhận file ảnh")
                                .build()
                );
            }

            // Validate file size (10MB = 10 * 1024 * 1024 bytes)
            long maxSize = 10 * 1024 * 1024; // 10MB
            if (file.getSize() > maxSize) {
                return ResponseEntity.badRequest().body(
                        com.example.food.dto.ApiResponse.builder()
                                .success(false)
                                .message("File quá lớn. Kích thước tối đa là 10MB")
                                .build()
                );
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ?
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String filename = UUID.randomUUID().toString() + extension;

            // Create upload directory if not exists
            Path uploadDir = Paths.get("uploads/avatars");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Save file
            Path filePath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Update user avatar URL
            User user = userService.getUserByEmail(userDetails.getUsername());
            String avatarUrl = "/uploads/avatars/" + filename;
            user.setAvatarUrl(avatarUrl);
            userService.saveUser(user);

            return ResponseEntity.ok(com.example.food.dto.ApiResponse.builder()
                    .success(true)
                    .message("Upload avatar thành công")
                    .data(avatarUrl)
                    .build());

        } catch (IOException e) {
            return ResponseEntity.badRequest().body(
                    com.example.food.dto.ApiResponse.builder()
                            .success(false)
                            .message("Lỗi upload file: " + e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    com.example.food.dto.ApiResponse.builder()
                            .success(false)
                            .message("Lỗi server: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Test endpoint
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("✅ User API đang hoạt động!");
    }
}
