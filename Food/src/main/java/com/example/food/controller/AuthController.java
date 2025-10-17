package com.example.food.controller;

import com.example.food.dto.*;
import com.example.food.model.User;
import com.example.food.repository.UserRepository;
import com.example.food.security.JwtTokenProvider;
import com.example.food.service.AuthService;
import com.example.food.service.OTPService;
import com.example.food.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private OTPService otpService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserService userService;

    // Store registration data temporarily (in production, use Redis or database)
    private Map<String, RegisterRequest> pendingRegistrations = new HashMap<>();

    /**
     * Step 1: Register - Send OTP
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@RequestBody RegisterRequest request) {
        // Store registration data temporarily
        pendingRegistrations.put(request.getEmail(), request);

        ApiResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Step 2: Verify OTP and complete registration
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOTP(@RequestBody OTPRequest otpRequest) {
        RegisterRequest registerRequest = pendingRegistrations.get(otpRequest.getEmail());

        if (registerRequest == null) {
            return ResponseEntity.badRequest().body(
                    AuthResponse.builder()
                            .success(false)
                            .message("Không tìm thấy thông tin đăng ký. Vui lòng đăng ký lại.")
                            .build()
            );
        }

        AuthResponse response = authService.verifyOTPAndCompleteRegistration(otpRequest, registerRequest);

        if (response.isSuccess()) {
            // Remove from temporary storage
            pendingRegistrations.remove(otpRequest.getEmail());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Resend OTP
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse> resendOTP(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        try {
            otpService.createAndSendOTP(email);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Mã OTP mới đã được gửi đến email của bạn.")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message("Không thể gửi OTP: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Login with email/password
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            AuthResponse response = authService.refreshAccessToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    AuthResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Google OAuth2 Success Handler
     */
    @GetMapping("/oauth2/success")
    public ResponseEntity<AuthResponse> oauth2Success(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            return ResponseEntity.badRequest().body(
                    AuthResponse.builder()
                            .success(false)
                            .message("Google authentication failed!")
                            .build()
            );
        }

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String picture = oauth2User.getAttribute("picture");

        // Check if user exists
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // Create new user from Google account
            user = new User();
            user.setEmail(email);
            user.setFullName(name);
            // Don't set avatar_url automatically - let user upload their own
            // user.setAvatarUrl(picture); // Commented out
            user.setPassword(passwordEncoder.encode("OAUTH2_USER_" + System.currentTimeMillis())); // Random password
            user.setRoleId(1); // USER role
            user = userRepository.save(user);
        }

        // Generate tokens
        String accessToken = jwtTokenProvider.generateToken(user.getEmail());
        String refreshToken = authService.createRefreshToken(user);

        return ResponseEntity.ok(AuthResponse.builder()
                .success(true)
                .message("Đăng nhập Google thành công!")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(userService.convertToDTO(user))
                .build());
    }

    /**
     * Get current user info
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(@AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(userService.convertToDTO(user));
    }

    /**
     * Google Login with ID Token (for mobile app)
     */
    @PostMapping("/google-login")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody Map<String, String> request) {
        try {
            String idToken = request.get("idToken");

            System.out.println("=== GOOGLE LOGIN REQUEST ===");
            System.out.println("ID Token received: " + (idToken != null ? "Yes" : "No"));

            if (idToken == null || idToken.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        AuthResponse.builder()
                                .success(false)
                                .message("ID Token không hợp lệ!")
                                .build()
                );
            }

            // Extract email from token
            String email = extractEmailFromToken(idToken);
            System.out.println("Email extracted: " + email);

            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        AuthResponse.builder()
                                .success(false)
                                .message("Không thể lấy email từ Google token!")
                                .build()
                );
            }

            // Check if user exists by email
            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                // Create new user from Google account
                System.out.println("Creating NEW user for email: " + email);
                user = new User();
                user.setEmail(email);
                user.setFullName("Google User"); // Default name, can extract from token
                user.setPassword(passwordEncoder.encode("GOOGLE_USER_" + System.currentTimeMillis()));
                user.setRoleId(1); // USER role
                user = userRepository.save(user);
                System.out.println("User created with ID: " + user.getUserId());
            } else {
                System.out.println("EXISTING user found with ID: " + user.getUserId());
                System.out.println("User email: " + user.getEmail());
            }

            // Generate tokens
            System.out.println("Generating JWT tokens...");
            String accessToken = jwtTokenProvider.generateToken(user.getEmail());
            String refreshToken = authService.createRefreshToken(user);
            System.out.println("Tokens generated successfully");

            AuthResponse response = AuthResponse.builder()
                    .success(true)
                    .message("Đăng nhập Google thành công!")
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .user(userService.convertToDTO(user))
                    .build();

            System.out.println("=== GOOGLE LOGIN SUCCESS ===");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("=== GOOGLE LOGIN ERROR ===");
            e.printStackTrace();
            return ResponseEntity.badRequest().body(
                    AuthResponse.builder()
                            .success(false)
                            .message("Lỗi đăng nhập Google: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Extract email from Google ID Token (simplified version)
     * TODO: Use Google API client library for proper verification
     */
    private String extractEmailFromToken(String idToken) {
        // Simplified: Split JWT and decode payload
        // Production should use Google's verification library
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length >= 2) {
                String payload = new String(java.util.Base64.getDecoder().decode(parts[1]));
                // Parse JSON to extract email
                if (payload.contains("email")) {
                    int emailStart = payload.indexOf("\"email\":\"") + 9;
                    int emailEnd = payload.indexOf("\"", emailStart);
                    return payload.substring(emailStart, emailEnd);
                }
            }
        } catch (Exception e) {
            // Fallback
        }
        return "google_user_" + System.currentTimeMillis() + "@gmail.com";
    }

    /**
     * Forgot Password - Send OTP to email
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            System.out.println("=== FORGOT PASSWORD REQUEST ===");
            System.out.println("Email: " + request.getEmail());

            // Check if user exists
            User user = userRepository.findByEmail(request.getEmail()).orElse(null);
            if (user == null) {
                System.out.println("❌ User not found: " + request.getEmail());
                return ResponseEntity.badRequest().body(
                        ApiResponse.builder()
                                .success(false)
                                .message("Email không tồn tại trong hệ thống")
                                .build()
                );
            }

            System.out.println("✓ User found: " + user.getEmail());

            // Generate and send OTP
            try {
                otpService.createAndSendOTP(request.getEmail());
                System.out.println("✓ OTP sent successfully");
            } catch (Exception e) {
                System.out.println("❌ Error sending OTP: " + e.getMessage());
                e.printStackTrace();

                // Fallback: Return success but with warning
                return ResponseEntity.ok(
                        ApiResponse.builder()
                                .success(true)
                                .message("Mã OTP đã được tạo. Vui lòng kiểm tra email hoặc liên hệ hỗ trợ.")
                                .data("OTP_SENT_WITH_WARNING")
                                .build()
                );
            }

            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .success(true)
                            .message("Mã OTP đã được gửi đến email của bạn")
                            .build()
            );

        } catch (Exception e) {
            System.out.println("❌ General error in forgot password: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .success(false)
                            .message("Lỗi server: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Reset Password - Verify OTP and set new password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            // Validate OTP
            if (!otpService.verifyOTP(request.getEmail(), request.getOtp())) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.builder()
                                .success(false)
                                .message("Mã OTP không đúng hoặc đã hết hạn")
                                .build()
                );
            }

            // Validate new password
            if (request.getNewPassword().length() < 6) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.builder()
                                .success(false)
                                .message("Mật khẩu mới phải có ít nhất 6 ký tự")
                                .build()
                );
            }

            // Validate password confirmation
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.builder()
                                .success(false)
                                .message("Mật khẩu xác nhận không khớp")
                                .build()
                );
            }

            // Find user and update password
            User user = userRepository.findByEmail(request.getEmail()).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.builder()
                                .success(false)
                                .message("Email không tồn tại trong hệ thống")
                                .build()
                );
            }

            // Update password
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            // Clear OTP after successful reset
            otpService.clearOTP(request.getEmail());

            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .success(true)
                            .message("Đặt lại mật khẩu thành công")
                            .build()
            );

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
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
        return ResponseEntity.ok("✅ Auth API đang hoạt động!");
    }

    /**
     * Test email service
     */
    @PostMapping("/test-email")
    public ResponseEntity<ApiResponse> testEmail(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.builder()
                                .success(false)
                                .message("Email không được để trống")
                                .build()
                );
            }

            // Test sending OTP
            otpService.createAndSendOTP(email);

            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .success(true)
                            .message("Test email sent successfully to: " + email)
                            .build()
            );

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .success(false)
                            .message("Test email failed: " + e.getMessage())
                            .build()
            );
        }
    }
}

