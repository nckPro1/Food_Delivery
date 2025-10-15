package com.example.food.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOTPEmail(String toEmail, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("FoodieExpress - Mã xác thực OTP");

            String htmlContent = "<!DOCTYPE html>" +
                    "<html>" +
                    "<head><meta charset='UTF-8'></head>" +
                    "<body style='font-family: Arial, sans-serif; padding: 20px; background-color: #f5f5f5;'>" +
                    "  <div style='max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);'>" +
                    "    <h1 style='color: #FF5722; text-align: center;'>🍕 FoodieExpress</h1>" +
                    "    <h2 style='color: #333;'>Xác thực tài khoản</h2>" +
                    "    <p style='font-size: 16px; color: #666;'>Cảm ơn bạn đã đăng ký tài khoản FoodieExpress!</p>" +
                    "    <p style='font-size: 16px; color: #666;'>Mã OTP của bạn là:</p>" +
                    "    <div style='text-align: center; margin: 30px 0;'>" +
                    "      <span style='font-size: 32px; font-weight: bold; color: #FF5722; background-color: #FFF3E0; padding: 15px 30px; border-radius: 8px; letter-spacing: 5px;'>" +
                    otpCode +
                    "      </span>" +
                    "    </div>" +
                    "    <p style='font-size: 14px; color: #999;'>Mã OTP có hiệu lực trong <strong>5 phút</strong>.</p>" +
                    "    <p style='font-size: 14px; color: #999;'>Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email này.</p>" +
                    "    <hr style='margin: 30px 0; border: none; border-top: 1px solid #eee;'>" +
                    "    <p style='font-size: 12px; color: #aaa; text-align: center;'>© 2025 FoodieExpress. All rights reserved.</p>" +
                    "  </div>" +
                    "</body>" +
                    "</html>";

            helper.setText(htmlContent, true);
            mailSender.send(message);

            System.out.println("OTP Email sent to: " + toEmail);

        } catch (MessagingException e) {
            System.err.println("Error sending OTP email: " + e.getMessage());
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }
}

