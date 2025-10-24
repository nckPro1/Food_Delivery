package com.example.food.service;

import com.example.food.model.OTP;
import com.example.food.repository.OTPRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class OTPService {

    @Autowired
    private OTPRepository otpRepository;

    @Autowired
    private EmailService emailService;

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;

    public String generateOTP() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    @Transactional
    public void createAndSendOTP(String email) {
        // Delete old OTPs for this email
        otpRepository.deleteByEmail(email);

        // Generate new OTP
        String otpCode = generateOTP();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        // Save OTP to database
        OTP otp = OTP.builder()
                .email(email)
                .otpCode(otpCode)
                .expiryTime(expiryTime)
                .verified(false)
                .createdAt(LocalDateTime.now())
                .build();

        otpRepository.save(otp);

        // Send OTP via email
        emailService.sendOTPEmail(email, otpCode);
    }

    public boolean verifyOTP(String email, String otpCode) {
        System.out.println("=== OTP VERIFICATION ===");
        System.out.println("Email: " + email);
        System.out.println("OTP Code: " + otpCode);

        Optional<OTP> otpOpt = otpRepository.findByEmailAndOtpCodeAndVerifiedFalse(email, otpCode);

        if (otpOpt.isEmpty()) {
            System.out.println("❌ OTP not found in database");
            return false;
        }

        OTP otp = otpOpt.get();
        System.out.println("✓ OTP found in database");
        System.out.println("OTP expiry time: " + otp.getExpiryTime());
        System.out.println("Current time: " + LocalDateTime.now());

        // Check if OTP is expired
        if (LocalDateTime.now().isAfter(otp.getExpiryTime())) {
            System.out.println("❌ OTP has expired");
            // Clean up expired OTP
            otpRepository.delete(otp);
            return false;
        }

        System.out.println("✓ OTP is valid and not expired");

        // Mark OTP as verified
        otp.setVerified(true);
        otpRepository.save(otp);

        System.out.println("✓ OTP marked as verified");
        return true;
    }

    @Transactional
    public void cleanupExpiredOTPs() {
        otpRepository.deleteByExpiryTimeBefore(LocalDateTime.now());
    }

    @Transactional
    public void clearOTP(String email) {
        otpRepository.deleteByEmail(email);
    }
}

