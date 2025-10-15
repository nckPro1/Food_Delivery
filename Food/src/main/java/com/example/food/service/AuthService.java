package com.example.food.service;

import com.example.food.dto.*;
import com.example.food.model.RefreshToken;
import com.example.food.model.User;
import com.example.food.repository.RefreshTokenRepository;
import com.example.food.repository.UserRepository;
import com.example.food.security.JwtTokenProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private OTPService otpService;
    @Autowired private UserService userService;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    /**
     * ✅ ĐÃ THÊM LẠI: Bước 1 của luồng đăng ký - Gửi OTP.
     * Kiểm tra email đã tồn tại chưa, sau đó gửi mã OTP.
     */
    @Transactional
    public ApiResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ApiResponse.builder()
                    .success(false)
                    .message("Email đã tồn tại!")
                    .build();
        }

        otpService.createAndSendOTP(request.getEmail());

        return ApiResponse.builder()
                .success(true)
                .message("Mã OTP đã được gửi đến email của bạn.")
                .build();
    }

    /**
     * ✅ ĐÃ THÊM LẠI: Bước 2 của luồng đăng ký - Xác thực OTP và tạo tài khoản.
     * Kiểm tra OTP, sau đó tạo và lưu người dùng mới vào database.
     */
    @Transactional
    public AuthResponse verifyOTPAndCompleteRegistration(OTPRequest otpRequest, RegisterRequest registerRequest) {
        boolean isValid = otpService.verifyOTP(otpRequest.getEmail(), otpRequest.getOtp());

        if (!isValid) {
            return AuthResponse.builder()
                    .success(false)
                    .message("Mã OTP không hợp lệ hoặc đã hết hạn!")
                    .build();
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return AuthResponse.builder()
                    .success(false)
                    .message("Email này đã được đăng ký!")
                    .build();
        }

        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFullName(registerRequest.getFullName());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setRoleId(1); // Default USER role
        userRepository.save(user);

        return AuthResponse.builder()
                .success(true)
                .message("Đăng ký thành công! Vui lòng đăng nhập.")
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new RuntimeException("User not found after auth"));
        String accessToken = jwtTokenProvider.generateToken(user.getEmail());
        String refreshToken = createRefreshToken(user);
        return AuthResponse.builder().success(true).message("Đăng nhập thành công!").accessToken(accessToken).refreshToken(refreshToken).tokenType("Bearer").user(userService.convertToDTO(user)).build();
    }

    @Transactional
    public AuthResponse loginWithGoogle(String idTokenString) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            throw new IllegalArgumentException("ID Token không hợp lệ hoặc đã hết hạn!");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String fullName = (String) payload.get("name");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFullName(fullName);
                    newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    newUser.setRoleId(1);
                    return userRepository.save(newUser);
                });

        String accessToken = jwtTokenProvider.generateToken(user.getEmail());
        String refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .success(true)
                .message("Đăng nhập Google thành công!")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(userService.convertToDTO(user))
                .build();
    }

    public String createRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user);
        String tokenValue = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(7);
        RefreshToken refreshToken = RefreshToken.builder().token(tokenValue).user(user).expiryDate(expiryDate).createdAt(LocalDateTime.now()).build();
        refreshTokenRepository.save(refreshToken);
        return tokenValue;
    }
}