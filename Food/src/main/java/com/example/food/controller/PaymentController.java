package com.example.food.controller;

import com.example.food.dto.*;
import com.example.food.service.PaymentService;
import com.example.food.service.VnpayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;
    private final VnpayService vnpayService;

    /**
     * Lấy danh sách phương thức thanh toán
     */
    @GetMapping("/methods")
    public ResponseEntity<ApiResponse<Object>> getPaymentMethods() {
        try {
            ApiResponse<Object> response = paymentService.getPaymentMethods();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting payment methods: ", e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.<Object>builder()
                            .success(false)
                            .message("Lỗi lấy danh sách phương thức thanh toán: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Tạo thanh toán cho đơn hàng (Cash on Delivery)
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Object>> createPayment(
            @RequestBody CreatePaymentRequest request,
            HttpServletRequest httpRequest) {
        try {
            log.info("Creating payment for order: {}", request.getOrderId());

            // Lấy IP address
            String ipAddress = getClientIpAddress(httpRequest);
            request.setIpAddress(ipAddress);

            ApiResponse<Object> response = paymentService.createPayment(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error creating payment: ", e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .success(false)
                            .message("Lỗi tạo thanh toán: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Tạo thanh toán VNPay
     */
    @PostMapping("/create-payment")
    public ResponseEntity<ApiResponse<VnpayPaymentResponse>> createVnpayPayment(
            @RequestBody CreateVnpayPaymentRequest request,
            HttpServletRequest httpRequest) {
        try {
            log.info("Creating VNPay payment for amount: {}", request.getAmount());

            // Lấy IP address của client
            String ipAddress = getClientIpAddress(httpRequest);
            request.setIpAddr(ipAddress);

            // Tạo URL thanh toán VNPay
            ApiResponse<VnpayPaymentResponse> response = vnpayService.createVnpayPayment(request);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Error creating VNPay payment: ", e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.<VnpayPaymentResponse>builder()
                            .success(false)
                            .message("Lỗi tạo thanh toán VNPay: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Xử lý callback từ VNPay
     */
    @GetMapping("/payment-callback")
    public ResponseEntity<String> vnpayCallback(HttpServletRequest request) {
        try {
            log.info("Received VNPay callback");

            // Lấy tất cả parameters từ request
            Map<String, String> params = new HashMap<>();
            request.getParameterMap().forEach((key, value) -> {
                if (value.length > 0) {
                    params.put(key, value[0]);
                }
            });

            // Xử lý callback
            ApiResponse<String> response = vnpayService.handleVnpayCallback(params);

            // Trả về HTML response
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(response.getData());

        } catch (Exception e) {
            log.error("Error processing VNPay callback: ", e);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body("<h1 style='color: red;'>Lỗi xử lý giao dịch</h1>");
        }
    }

    /**
     * Lấy thông tin thanh toán theo ID
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentDTO>> getPaymentById(@PathVariable Long paymentId) {
        try {
            Optional<PaymentDTO> payment = paymentService.getPaymentById(paymentId);
            if (payment.isPresent()) {
                return ResponseEntity.ok(
                        ApiResponse.<PaymentDTO>builder()
                                .success(true)
                                .message("Lấy thông tin thanh toán thành công")
                                .data(payment.get())
                                .build()
                );
            } else {
                return ResponseEntity.badRequest().body(
                        ApiResponse.<PaymentDTO>builder()
                                .success(false)
                                .message("Không tìm thấy thông tin thanh toán")
                                .build()
                );
            }
        } catch (Exception e) {
            log.error("Error getting payment by ID: ", e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.<PaymentDTO>builder()
                            .success(false)
                            .message("Lỗi lấy thông tin thanh toán: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Lấy thông tin thanh toán theo order ID
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentDTO>> getPaymentByOrderId(@PathVariable Long orderId) {
        try {
            Optional<PaymentDTO> payment = paymentService.getPaymentByOrderId(orderId);
            if (payment.isPresent()) {
                return ResponseEntity.ok(
                        ApiResponse.<PaymentDTO>builder()
                                .success(true)
                                .message("Lấy thông tin thanh toán thành công")
                                .data(payment.get())
                                .build()
                );
            } else {
                return ResponseEntity.badRequest().body(
                        ApiResponse.<PaymentDTO>builder()
                                .success(false)
                                .message("Không tìm thấy thông tin thanh toán")
                                .build()
                );
            }
        } catch (Exception e) {
            log.error("Error getting payment by order ID: ", e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.<PaymentDTO>builder()
                            .success(false)
                            .message("Lỗi lấy thông tin thanh toán: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Lấy IP address của client
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null) {
            return request.getRemoteAddr();
        } else {
            return xForwardedForHeader.split(",")[0];
        }
    }
}