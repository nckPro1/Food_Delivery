package com.example.food.service;

import com.example.food.config.VnpayConfig;
import com.example.food.dto.ApiResponse;
import com.example.food.dto.CreateVnpayPaymentRequest;
import com.example.food.dto.VnpayPaymentResponse;
import com.example.food.model.Order;
import com.example.food.model.Payment;
import com.example.food.repository.OrderRepository;
import com.example.food.repository.PaymentRepository;
import com.example.food.util.VnpayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VnpayService {

    private final VnpayConfig vnpayConfig;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    /**
     * Tạo URL thanh toán VNPay
     */
    @Transactional
    public ApiResponse<VnpayPaymentResponse> createVnpayPayment(CreateVnpayPaymentRequest request) {
        try {
            log.info("Creating VNPay payment for amount: {}", request.getAmount());

            // Tạo các tham số VNPay
            Map<String, String> vnpParams = new HashMap<>();
            vnpParams.put("vnp_Version", VnpayConfig.VNP_VERSION);
            vnpParams.put("vnp_Command", VnpayConfig.VNP_COMMAND);
            vnpParams.put("vnp_TmnCode", vnpayConfig.getVnpTmnCode());
            vnpParams.put("vnp_Amount", String.valueOf(request.getAmount() * 100)); // VNPay yêu cầu nhân 100
            vnpParams.put("vnp_CurrCode", VnpayConfig.VNP_CURRENCY_CODE);
            vnpParams.put("vnp_TxnRef", VnpayUtil.getRandomNumber(8)); // Mã giao dịch duy nhất
            vnpParams.put("vnp_OrderInfo", request.getOrderInfo() != null ? request.getOrderInfo() : "Thanh toan don hang");
            vnpParams.put("vnp_OrderType", VnpayConfig.VNP_ORDER_TYPE);
            vnpParams.put("vnp_Locale", VnpayConfig.VNP_LOCALE);
            vnpParams.put("vnp_ReturnUrl", vnpayConfig.getVnpReturnUrl());
            vnpParams.put("vnp_IpAddr", request.getIpAddr());

            // Thời gian tạo và hết hạn
            String createDate = VnpayUtil.getDate("yyyyMMddHHmmss");
            vnpParams.put("vnp_CreateDate", createDate);

            // Tạo secure hash
            String hashData = VnpayUtil.getHashData(vnpParams);
            String vnpSecureHash = VnpayUtil.hmacSHA512(vnpayConfig.getVnpHashSecret(), hashData);
            vnpParams.put("vnp_SecureHash", vnpSecureHash);

            // Tạo URL thanh toán
            String queryString = VnpayUtil.getQueryString(vnpParams);
            String paymentUrl = vnpayConfig.getVnpUrl() + "?" + queryString;

            // Lưu thông tin payment vào database nếu có orderId
            if (request.getOrderId() != null) {
                saveVnpayPaymentRecord(request, vnpParams.get("vnp_TxnRef"));
            }

            VnpayPaymentResponse response = VnpayPaymentResponse.builder()
                    .paymentUrl(paymentUrl)
                    .txnRef(vnpParams.get("vnp_TxnRef"))
                    .amount(request.getAmount())
                    .orderInfo(vnpParams.get("vnp_OrderInfo"))
                    .build();

            return ApiResponse.<VnpayPaymentResponse>builder()
                    .success(true)
                    .message("Tạo URL thanh toán VNPay thành công")
                    .data(response)
                    .build();

        } catch (Exception e) {
            log.error("Error creating VNPay payment: ", e);
            return ApiResponse.<VnpayPaymentResponse>builder()
                    .success(false)
                    .message("Lỗi tạo thanh toán VNPay: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Xử lý callback từ VNPay
     */
    @Transactional
    public ApiResponse<String> handleVnpayCallback(Map<String, String> params) {
        try {
            log.info("Processing VNPay callback with params: {}", params);

            // Lấy các tham số cần thiết
            String vnpSecureHash = params.get("vnp_SecureHash");
            String vnpTxnRef = params.get("vnp_TxnRef");
            String vnpResponseCode = params.get("vnp_ResponseCode");
            String vnpTxnStatus = params.get("vnp_TransactionStatus");
            String vnpAmount = params.get("vnp_Amount");

            // Xóa vnp_SecureHash khỏi params để tính toán hash
            params.remove("vnp_SecureHash");
            params.remove("vnp_SecureHashType");

            // Tính toán secure hash để xác thực
            String hashData = VnpayUtil.getHashData(params);
            String calculatedHash = VnpayUtil.hmacSHA512(vnpayConfig.getVnpHashSecret(), hashData);

            if (!calculatedHash.equals(vnpSecureHash)) {
                log.error("Invalid secure hash. Expected: {}, Actual: {}", calculatedHash, vnpSecureHash);
                return ApiResponse.<String>builder()
                        .success(false)
                        .message("Chữ ký không hợp lệ")
                        .data("<h1 style='color: red;'>Giao dịch thất bại - Chữ ký không hợp lệ</h1>")
                        .build();
            }

            // Cập nhật trạng thái payment
            updatePaymentStatus(vnpTxnRef, vnpResponseCode, vnpTxnStatus, params);

            if ("00".equals(vnpResponseCode) && "00".equals(vnpTxnStatus)) {
                log.info("VNPay payment successful for txnRef: {}", vnpTxnRef);
                return ApiResponse.<String>builder()
                        .success(true)
                        .message("Giao dịch thành công")
                        .data("<h1 style='color: green;'>Giao dịch thành công</h1>" +
                                "<p>Mã giao dịch: " + vnpTxnRef + "</p>" +
                                "<p>Số tiền: " + Long.parseLong(vnpAmount) / 100 + " VND</p>")
                        .build();
            } else {
                log.info("VNPay payment failed for txnRef: {} with response code: {}", vnpTxnRef, vnpResponseCode);
                return ApiResponse.<String>builder()
                        .success(false)
                        .message("Giao dịch thất bại")
                        .data("<h1 style='color: red;'>Giao dịch thất bại</h1>" +
                                "<p>Mã lỗi: " + vnpResponseCode + "</p>" +
                                "<p>Mã giao dịch: " + vnpTxnRef + "</p>")
                        .build();
            }

        } catch (Exception e) {
            log.error("Error processing VNPay callback: ", e);
            return ApiResponse.<String>builder()
                    .success(false)
                    .message("Lỗi xử lý callback")
                    .data("<h1 style='color: red;'>Lỗi xử lý giao dịch</h1>")
                    .build();
        }
    }

    /**
     * Lưu thông tin payment vào database
     */
    private void saveVnpayPaymentRecord(CreateVnpayPaymentRequest request, String txnRef) {
        try {
            Order order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại"));

            Payment payment = Payment.builder()
                    .order(order)
                    .paymentMethod("VNPAY")
                    .paymentAmount(BigDecimal.valueOf(request.getAmount()))
                    .paymentStatus(Payment.PaymentStatus.PENDING)
                    .transactionId(txnRef)
                    .paymentDate(LocalDateTime.now())
                    .paymentNotes("VNPay payment - " + request.getOrderInfo())
                    .build();

            paymentRepository.save(payment);
            log.info("Saved VNPay payment record with txnRef: {}", txnRef);

        } catch (Exception e) {
            log.error("Error saving VNPay payment record: ", e);
        }
    }

    /**
     * Cập nhật trạng thái payment sau callback
     */
    private void updatePaymentStatus(String txnRef, String responseCode, String txnStatus, Map<String, String> params) {
        try {
            Payment payment = paymentRepository.findByTransactionId(txnRef).orElse(null);
            if (payment != null) {
                boolean ok = "00".equals(responseCode) && "00".equals(txnStatus);
                if (ok) {
                    payment.setPaymentStatus(Payment.PaymentStatus.COMPLETED);
                    payment.getOrder().setPaymentStatus(Order.PaymentStatus.COMPLETED);
                    // Mark order as DONE for delivery flow
                    payment.getOrder().setOrderStatus(Order.OrderStatus.DONE);
                } else {
                    payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
                    payment.getOrder().setPaymentStatus(Order.PaymentStatus.FAILED);
                }

                payment.setPaymentNotes("VNPay response: " + responseCode + " - " + txnStatus);
                paymentRepository.save(payment);
                orderRepository.save(payment.getOrder());

                log.info("Updated payment status for txnRef: {} to status: {}", txnRef,
                        (ok ? "COMPLETED" : "FAILED"));
            }

        } catch (Exception e) {
            log.error("Error updating payment status for txnRef: {}", txnRef, e);
        }
    }
}