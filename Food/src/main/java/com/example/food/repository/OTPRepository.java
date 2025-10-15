package com.example.food.repository;

import com.example.food.model.OTP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OTPRepository extends JpaRepository<OTP, Long> {

    Optional<OTP> findByEmailAndOtpCodeAndVerifiedFalse(String email, String otpCode);

    Optional<OTP> findTopByEmailAndVerifiedFalseOrderByCreatedAtDesc(String email);

    void deleteByExpiryTimeBefore(LocalDateTime dateTime);

    void deleteByEmail(String email);
}

