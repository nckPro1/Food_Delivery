package com.example.food.service;

import com.example.food.dto.*;
import com.example.food.model.AuthProvider;
import com.example.food.model.RefreshToken;
import com.example.food.model.User;
import com.example.food.repository.RefreshTokenRepository;
import com.example.food.repository.UserRepository;
import com.example.food.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private OTPService otpService;

    @Autowired
    private UserService userService;

    @Transactional
    public ApiResponse register(RegisterRequest request) {
        // Check if email already exists with EMAIL auth provider
        User existingUser = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (existingUser != null && existingUser.getAuthProvider() == AuthProvider.EMAIL) {
            return ApiResponse.builder()
                    .success(false)
                    .message("Email đã được đăng ký với phương thức đăng ký thông thường!")
                    .build();
        }

        // If user exists with Google auth provider, allow registration to merge accounts
        if (existingUser != null && existingUser.getAuthProvider() == AuthProvider.GOOGLE) {
            return ApiResponse.builder()
                    .success(true)
                    .message("Email này đã được đăng ký bằng Google. Bạn có thể tiếp tục để liên kết tài khoản.")
                    .data("ACCOUNT_MERGE_REQUIRED")
                    .build();
        }

        // Create new user (not activated yet)
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRoleId(1); // Default USER role
        user.setAuthProvider(AuthProvider.EMAIL); // Set auth provider

        // Don't save user yet, wait for OTP verification

        // Send OTP
        otpService.createAndSendOTP(request.getEmail());

        return ApiResponse.builder()
                .success(true)
                .message("Mã OTP đã được gửi đến email của bạn. Vui lòng xác thực để hoàn tất đăng ký.")
                .data(request.getEmail())
                .build();
    }

    @Transactional
    public AuthResponse verifyOTPAndCompleteRegistration(OTPRequest otpRequest, RegisterRequest registerRequest) {
        // Verify OTP
        boolean isValid = otpService.verifyOTP(otpRequest.getEmail(), otpRequest.getOtp());

        if (!isValid) {
            return AuthResponse.builder()
                    .success(false)
                    .message("Mã OTP không hợp lệ hoặc đã hết hạn!")
                    .build();
        }

        // Check if user already exists with EMAIL auth provider
        User existingUser = userRepository.findByEmail(otpRequest.getEmail()).orElse(null);
        if (existingUser != null && existingUser.getAuthProvider() == AuthProvider.EMAIL) {
            return AuthResponse.builder()
                    .success(false)
                    .message("Email đã được đăng ký với phương thức đăng ký thông thường!")
                    .build();
        }

        // Handle account merge or create new user
        User user;
        if (existingUser != null && existingUser.getAuthProvider() == AuthProvider.GOOGLE) {
            // Merge with existing Google account
            user = existingUser;
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            user.setFullName(registerRequest.getFullName());
            user.setPhoneNumber(registerRequest.getPhoneNumber());
            // Keep Google auth provider but allow email login
            user.setAuthProvider(AuthProvider.EMAIL); // Switch to EMAIL auth provider
        } else {
            // Create new user
            user = new User();
            user.setEmail(registerRequest.getEmail());
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            user.setFullName(registerRequest.getFullName());
            user.setPhoneNumber(registerRequest.getPhoneNumber());
            user.setRoleId(1);
            user.setAuthProvider(AuthProvider.EMAIL); // Set auth provider
        }

        userRepository.save(user);

        // KHÔNG generate tokens - user phải login sau
        return AuthResponse.builder()
                .success(true)
                .message("Đăng ký thành công! Vui lòng đăng nhập.")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        System.out.println("=== LOGIN ATTEMPT ===");
        System.out.println("Email: " + request.getEmail());
        System.out.println("Password length: " + request.getPassword().length());

        try {
            // Load user from DB first to check
            User user = userRepository.findByEmail(request.getEmail()).orElse(null);
            if (user == null) {
                System.out.println("❌ ERROR: User not found!");
                return AuthResponse.builder()
                        .success(false)
                        .message("Email không tồn tại!")
                        .build();
            }

            // Check if user can login with email/password
            if (user.getAuthProvider() == AuthProvider.GOOGLE) {
                System.out.println("❌ ERROR: User registered with Google, please use Google login!");
                return AuthResponse.builder()
                        .success(false)
                        .message("Tài khoản này được đăng ký bằng Google. Vui lòng sử dụng đăng nhập Google!")
                        .build();
            }

            // Check if user has a valid password (not Google-generated)
            if (user.getPassword().startsWith("GOOGLE_USER_") || user.getPassword().startsWith("OAUTH2_USER_")) {
                System.out.println("❌ ERROR: User has Google-generated password, please use Google login!");
                return AuthResponse.builder()
                        .success(false)
                        .message("Tài khoản này chỉ hỗ trợ đăng nhập Google. Vui lòng sử dụng đăng nhập Google!")
                        .build();
            }

            System.out.println("✓ User found: " + user.getEmail());
            System.out.println("✓ Password in DB starts with: " + user.getPassword().substring(0, Math.min(20, user.getPassword().length())) + "...");
            System.out.println("✓ Password DB length: " + user.getPassword().length());

            // Test password match BEFORE Spring Security authentication
            boolean passwordMatch = passwordEncoder.matches(request.getPassword(), user.getPassword());
            System.out.println("✓ Password match (manual check): " + passwordMatch);

            if (!passwordMatch) {
                System.out.println("❌ Password mismatch detected!");
                System.out.println("   Input password: [hidden]");
                System.out.println("   DB hash: " + user.getPassword().substring(0, 30) + "...");
                return AuthResponse.builder()
                        .success(false)
                        .message("Mật khẩu không đúng!")
                        .build();
            }

            // Authenticate user with Spring Security
            System.out.println("→ Proceeding to Spring Security authentication...");
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            System.out.println("✓ Spring Security authentication SUCCESS!");

            // Generate tokens
            String accessToken = jwtTokenProvider.generateToken(user.getEmail());
            String refreshToken = createRefreshToken(user);

            System.out.println("✓ Tokens generated successfully");
            System.out.println("=== LOGIN SUCCESS ===");

            return AuthResponse.builder()
                    .success(true)
                    .message("Đăng nhập thành công!")
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .user(userService.convertToDTO(user))
                    .build();

        } catch (Exception e) {
            System.out.println("❌ LOGIN EXCEPTION ===");
            System.out.println("Exception type: " + e.getClass().getName());
            System.out.println("Message: " + e.getMessage());
            e.printStackTrace();
            return AuthResponse.builder()
                    .success(false)
                    .message("Email hoặc mật khẩu không đúng!")
                    .build();
        }
    }

    @Transactional
    public String createRefreshToken(User user) {
        try {
            // Delete old refresh tokens
            refreshTokenRepository.deleteByUser(user);

            // Create new refresh token
            String tokenValue = UUID.randomUUID().toString();
            LocalDateTime expiryDate = LocalDateTime.now().plusDays(7);

            RefreshToken refreshToken = RefreshToken.builder()
                    .token(tokenValue)
                    .user(user)
                    .expiresAt(expiryDate)
                    .build();

            refreshTokenRepository.save(refreshToken);

            return tokenValue;
        } catch (Exception e) {
            log.error("Error creating refresh token for user {}: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("Failed to create refresh token", e);
        }
    }

    public AuthResponse refreshAccessToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Refresh token không hợp lệ!"));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token đã hết hạn!");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtTokenProvider.generateToken(user.getEmail());

        return AuthResponse.builder()
                .success(true)
                .message("Token đã được làm mới!")
                .accessToken(newAccessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .user(userService.convertToDTO(user))
                .build();
    }
}

