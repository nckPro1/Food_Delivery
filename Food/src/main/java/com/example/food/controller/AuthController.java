package com.example.food.controller;

import com.example.food.dto.*;
import com.example.food.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // For development only
public class AuthController {

    @Autowired
    private AuthService authService;

    // Store registration data temporarily (in production, use Redis or database)
    private final Map<String, RegisterRequest> pendingRegistrations = new HashMap<>();

    // ... các endpoint /register, /verify-otp, /resend-otp, /login, /refresh của bạn giữ nguyên ...
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@RequestBody RegisterRequest request) {
        pendingRegistrations.put(request.getEmail(), request);
        ApiResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOTP(@RequestBody OTPRequest otpRequest) {
        RegisterRequest registerRequest = pendingRegistrations.get(otpRequest.getEmail());
        if (registerRequest == null) {
            return ResponseEntity.badRequest().body(AuthResponse.builder().success(false).message("Vui lòng đăng ký lại.").build());
        }
        AuthResponse response = authService.verifyOTPAndCompleteRegistration(otpRequest, registerRequest);
        if (response.isSuccess()) {
            pendingRegistrations.remove(otpRequest.getEmail());
        }
        return ResponseEntity.ok(response);
    }
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    // ... các endpoint khác

    /**
     * ✅ ĐÃ SỬA LẠI HOÀN TOÀN
     * Endpoint để đăng nhập bằng Google ID Token từ ứng dụng di động.
     * 1. Đổi tên thành /google cho ngắn gọn.
     * 2. Sử dụng GoogleLoginRequest DTO để nhận idToken.
     * 3. Giao toàn bộ logic xác thực và xử lý cho AuthService.
     */
    @PostMapping("/google")
    public ResponseEntity<?> loginWithGoogle(@RequestBody GoogleLoginRequest request) {
        try {
            AuthResponse authResponse = authService.loginWithGoogle(request.getIdToken());
            return ResponseEntity.ok(authResponse);
        } catch (IllegalArgumentException e) {
            // Lỗi do token không hợp lệ
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, e.getMessage(), null));
        } catch (Exception e) {
            // Các lỗi khác
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi server: " + e.getMessage(), null));
        }
    }

    // Endpoint /oauth2/success dùng cho web flow, không cần thay đổi
    // Endpoint /me và /test của bạn cũng giữ nguyên
}