
package com.example.food.service;

import com.example.food.dto.*;
import com.example.food.model.Order;
import com.example.food.model.Payment;
import com.example.food.repository.OrderRepository;
import com.example.food.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    /**
     * Tạo thanh toán cho đơn hàng
     */
    @Transactional
    public ApiResponse<Object> createPayment(CreatePaymentRequest request) {
        try {
            log.info("Creating payment for order: {}", request.getOrderId());

            // Kiểm tra đơn hàng tồn tại
            Order order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại"));

            // Kiểm tra trạng thái đơn hàng
            if (order.getOrderStatus() != Order.OrderStatus.PENDING) {
                throw new IllegalStateException("Đơn hàng không thể thanh toán");
            }

            // Kiểm tra phương thức thanh toán: KHÔNG ĐƯỢC ĐỂ NULL
            if (request.getPaymentMethod() == null || request.getPaymentMethod().trim().isEmpty()) {
                return ApiResponse.builder()
                        .success(false)
                        .message("Phương thức thanh toán không được để trống!")
                        .build();
            }

            // Luôn sử dụng số tiền tính từ server (bao gồm options, phí ship, coupon)
            BigDecimal serverAmount = order.getFinalAmount();
            if (serverAmount == null) {
                order.calculateFinalAmount();
                serverAmount = order.getFinalAmount();
            }

            // Tạo payment record với số tiền từ server thay vì client gửi lên
            Payment payment = Payment.builder()
                    .order(order)
                    .paymentMethod(request.getPaymentMethod())
                    .paymentAmount(serverAmount)
                    .paymentStatus(Payment.PaymentStatus.PENDING)
                    .paymentDate(LocalDateTime.now())
                    .build();

            payment = paymentRepository.save(payment);

            // Xử lý theo phương thức thanh toán
            switch (request.getPaymentMethod().toUpperCase()) {
                case "CASH":
                    return createCashPayment(payment);
                case "E_WALLET":
                    return createEWalletPayment(payment);
                case "VNPAY":
                    return createVnpayPaymentRedirect(payment, request.getIpAddress());
                default:
                    throw new IllegalArgumentException("Phương thức thanh toán không được hỗ trợ: " + request.getPaymentMethod());
            }

        } catch (Exception e) {
            log.error("Error creating payment: ", e);
            return ApiResponse.builder()
                    .success(false)
                    .message("Lỗi tạo thanh toán: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Tạo thanh toán tiền mặt
     */
    private ApiResponse<Object> createCashPayment(Payment payment) {
        try {
            // Không set COMPLETED nữa, chỉ để PENDING!
            payment.setPaymentStatus(Payment.PaymentStatus.PENDING);
            payment.setPaymentDate(LocalDateTime.now());
            payment.setPaymentNotes("Khách chọn thanh toán tiền mặt khi nhận hàng");
            paymentRepository.save(payment);

            Order order = payment.getOrder();
            order.setPaymentStatus(Order.PaymentStatus.PENDING); // Không set COMPLETED nữa
            orderRepository.save(order);

            return ApiResponse.builder()
                    .success(true)
                    .message("Tạo thanh toán tiền mặt thành công (chờ giao hàng mới hoàn thành)")
                    .build();
        } catch (Exception e) {
            log.error("Error creating cash payment: ", e);
            return ApiResponse.builder()
                    .success(false)
                    .message("Lỗi tạo thanh toán tiền mặt: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Tạo thanh toán E_WALLET (tự động hoàn thành thanh toán nhưng order vẫn chờ xử lý)
     * QUAN TRỌNG: Chỉ set payment status = COMPLETED, KHÔNG thay đổi order status (giữ nguyên PENDING)
     */
    private ApiResponse<Object> createEWalletPayment(Payment payment) {
        try {
            Order order = payment.getOrder();
            Order.OrderStatus originalOrderStatus = order.getOrderStatus(); // Lưu trạng thái ban đầu

            // E_WALLET tự động complete payment nhưng order vẫn ở trạng thái chờ xử lý
            payment.setPaymentStatus(Payment.PaymentStatus.COMPLETED);
            payment.setPaymentDate(LocalDateTime.now());
            payment.setPaymentNotes("Thanh toán qua ví điện tử");
            paymentRepository.save(payment);

            // CHỈ set payment status của order, KHÔNG thay đổi order status
            order.setPaymentStatus(Order.PaymentStatus.COMPLETED);

            // Đảm bảo order status vẫn là PENDING (chờ xử lý), không set thành DONE
            // Nếu order status đã bị thay đổi, set lại về PENDING
            if (order.getOrderStatus() != Order.OrderStatus.PENDING) {
                log.warn("Order {} had status {} but should be PENDING for e-wallet payment. Resetting to PENDING.",
                        order.getOrderId(), order.getOrderStatus());
                order.setOrderStatus(Order.OrderStatus.PENDING);
            }

            orderRepository.save(order);

            log.info("E-Wallet payment completed for order {}. Payment status: COMPLETED, Order status: {} (PENDING)",
                    order.getOrderId(), order.getOrderStatus());

            return ApiResponse.builder()
                    .success(true)
                    .message("Thanh toán ví điện tử thành công")
                    .build();
        } catch (Exception e) {
            log.error("Error creating E_WALLET payment: ", e);
            return ApiResponse.builder()
                    .success(false)
                    .message("Lỗi tạo thanh toán ví điện tử: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Tạo thanh toán VNPay (chuyển hướng đến VNPay)
     */
    private ApiResponse<Object> createVnpayPaymentRedirect(Payment payment, String ipAddress) {
        try {
            // Thông báo rằng cần redirect đến VNPay
            // Frontend sẽ gọi API /api/payment/create-payment để lấy URL thanh toán
            return ApiResponse.builder()
                    .success(true)
                    .message("Cần chuyển hướng đến VNPay")
                    .data(Map.of(
                            "paymentId", payment.getPaymentId(),
                            "orderId", payment.getOrder().getOrderId(),
                            "amount", payment.getPaymentAmount().longValue(),
                            "redirectToVnpay", true
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Error creating VNPay payment redirect: ", e);
            return ApiResponse.builder()
                    .success(false)
                    .message("Lỗi tạo thanh toán VNPay: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Lấy danh sách phương thức thanh toán
     */
    public ApiResponse<Object> getPaymentMethods() {
        try {
            List<PaymentMethodDTO> methods = new ArrayList<>();

            // Tiền mặt
            PaymentMethodDTO cash = PaymentMethodDTO.builder()
                    .methodId("CASH")
                    .methodName("Tiền mặt")
                    .description("Thanh toán khi nhận hàng")
                    .iconUrl("/images/payment/cash.png")
                    .isActive(true)
                    .isOnline(false)
                    .build();
            methods.add(cash);

            // VNPay
            PaymentMethodDTO vnpay = PaymentMethodDTO.builder()
                    .methodId("VNPAY")
                    .methodName("VNPay")
                    .description("Thanh toán trực tuyến qua VNPay")
                    .iconUrl("/images/payment/vnpay.png")
                    .isActive(true)
                    .isOnline(true)
                    .build();
            methods.add(vnpay);

            return ApiResponse.builder()
                    .success(true)
                    .message("Lấy danh sách phương thức thanh toán thành công")
                    .data(methods)
                    .build();

        } catch (Exception e) {
            log.error("Error getting payment methods: ", e);
            return ApiResponse.builder()
                    .success(false)
                    .message("Lỗi lấy danh sách phương thức thanh toán: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Lấy thông tin thanh toán theo ID
     */
    public Optional<PaymentDTO> getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .map(this::convertToDTO);
    }

    /**
     * Lấy thông tin thanh toán theo order ID
     */
    public Optional<PaymentDTO> getPaymentByOrderId(Long orderId) {
        List<Payment> payments = paymentRepository.findByOrderOrderId(orderId);
        if (payments.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(convertToDTO(payments.get(0)));
    }

    /**
     * Convert Payment entity to DTO
     */
    private PaymentDTO convertToDTO(Payment payment) {
        return PaymentDTO.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrder().getOrderId())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus().toString())
                .paymentAmount(payment.getPaymentAmount())
                .transactionId(payment.getTransactionId())
                .paymentNotes(payment.getPaymentNotes())
                .paymentDate(payment.getPaymentDate())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}