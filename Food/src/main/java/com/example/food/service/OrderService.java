package com.example.food.service;

import com.example.food.dto.*;
import com.example.food.model.*;
import com.example.food.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderItemOptionRepository orderItemOptionRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final CouponService couponService;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final ShippingFeeSettingsService shippingFeeSettingsService;

    @Autowired(required = false)
    private com.example.food.service.NotificationService notificationService;

    // ===============================
    // ORDER MANAGEMENT
    // ===============================

    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());

        // Validate request
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        // Validate user
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + request.getUserId()));

        // Validate delivery address - use from request if provided, otherwise fallback to user's address
        String deliveryAddress = request.getDeliveryAddress();
        if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
            // Fallback to user's address if no delivery address provided
            if (user.getAddress() == null || user.getAddress().trim().isEmpty()) {
                throw new IllegalArgumentException("User must have a delivery address");
            }
            deliveryAddress = user.getAddress();
        }

        // Validate payment method
        if (request.getPaymentMethod() == null || request.getPaymentMethod().trim().isEmpty()) {
            throw new IllegalArgumentException("Bạn phải chọn phương thức thanh toán!");
        }

        List<String> couponCodes = request.getCouponCodes();
        BigDecimal baseSubtotal = calculateTotalAmount(request.getOrderItems());
        BigDecimal couponDiscount = BigDecimal.ZERO;
        List<String> appliedCoupons = new ArrayList<>();
        if (couponCodes != null && !couponCodes.isEmpty()) {
            for (String code : couponCodes) {
                Optional<Coupon> couponOpt = couponRepository.findValidCoupon(code, LocalDateTime.now());
                if (couponOpt.isPresent() && couponOpt.get().canBeUsedForOrder(baseSubtotal)) {
                    Coupon coupon = couponOpt.get();
                    BigDecimal thisDiscount = coupon.calculateDiscount(baseSubtotal);
                    if (thisDiscount.compareTo(BigDecimal.ZERO) > 0) {
                        couponDiscount = couponDiscount.add(thisDiscount);
                        appliedCoupons.add(code);
                    }
                }
            }
            // Do not allow total coupon discount to exceed order subtotal
            if (couponDiscount.compareTo(baseSubtotal) > 0) {
                couponDiscount = baseSubtotal;
            }
        }

        // Create order
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .orderStatus(Order.OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .paymentMethod(Order.PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()))
                .deliveryAddress(deliveryAddress)
                .deliveryNotes(request.getDeliveryNotes())
                .couponDiscount(couponDiscount)
                .discountAmount(couponDiscount)
                .appliedCouponCodes(appliedCoupons)
                .build();

        // Calculate order items and total
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = request.getOrderItems().stream()
                .map(itemRequest -> {
                    Product product = productRepository.findById(itemRequest.getProductId())
                            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemRequest.getProductId()));

                    OrderItem orderItem = OrderItem.builder()
                            .order(order)
                            .product(product)
                            .quantity(itemRequest.getQuantity())
                            .unitPrice(product.getPrice())
                            .salePrice(product.getCurrentPrice())
                            .specialInstructions(itemRequest.getSpecialInstructions())
                            .build();

                    // Calculate total price with options
                    orderItem.calculateTotalPrice();

                    // Add selected options
                    if (itemRequest.getSelectedOptionIds() != null) {
                        List<OrderItemOption> options = itemRequest.getSelectedOptionIds().stream()
                                .map(optionId -> {
                                    ProductOption productOption = productOptionRepository.findById(optionId)
                                            .orElseThrow(() -> new IllegalArgumentException("Product option not found: " + optionId));

                                    return OrderItemOption.builder()
                                            .orderItem(orderItem)
                                            .productOption(productOption)
                                            .optionName(productOption.getOptionName())
                                            .optionType(productOption.getOptionType())
                                            .price(productOption.getPrice())
                                            .build();
                                })
                                .collect(Collectors.toList());

                        orderItem.setOrderItemOptions(options);
                        orderItem.calculateTotalPrice(); // Recalculate with options
                    }

                    return orderItem;
                })
                .collect(Collectors.toList());

        // Calculate total amount
        totalAmount = orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(totalAmount);

        // Calculate shipping fee using ShippingFeeSettingsService
        BigDecimal shippingFee = shippingFeeSettingsService.calculateShippingFee(totalAmount);
        order.setShippingFee(shippingFee);

        // Calculate final amount
        BigDecimal finalAmount = totalAmount.add(shippingFee).subtract(couponDiscount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }
        order.setFinalAmount(finalAmount);

        // Set estimated delivery time
        order.setEstimatedDeliveryTime(LocalDateTime.now().plusMinutes(30));

        // Save order
        Order savedOrder = orderRepository.save(order);

        // Ensure finalAmount is calculated after save
        if (savedOrder.getFinalAmount() == null) {
            savedOrder.calculateFinalAmount();
            savedOrder = orderRepository.save(savedOrder);
        }

        // Create final reference for lambda
        final Order finalSavedOrder = savedOrder;

        // Save order items
        orderItems.forEach(item -> {
            item.setOrder(finalSavedOrder);
            OrderItem savedItem = orderItemRepository.save(item);

            // Save order item options
            if (item.getOrderItemOptions() != null) {
                item.getOrderItemOptions().forEach(option -> {
                    option.setOrderItem(savedItem);
                    orderItemOptionRepository.save(option);
                });
            }
        });

        // Create payment record
        Payment payment = Payment.builder()
                .order(finalSavedOrder)
                .paymentMethod(request.getPaymentMethod())
                .paymentAmount(finalSavedOrder.getFinalAmount())
                .paymentStatus(Payment.PaymentStatus.PENDING)
                .paymentDate(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        // Convert to DTO for response
        OrderDTO orderDTO = convertToDTO(finalSavedOrder);

        // Gửi notification cho admin về đơn hàng mới
        if (notificationService != null) {
            try {
                notificationService.notifyNewOrder(orderDTO);
            } catch (Exception e) {
                log.error("Error sending new order notification: {}", e.getMessage(), e);
            }
        }

        log.info("Order created successfully: {}", finalSavedOrder.getOrderNumber());
        return orderDTO;
    }

    public List<OrderDTO> getOrdersByUserId(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return orders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<OrderDTO> getAllOrders() {
        List<Order> orders = orderRepository.findAllOrdersOrderByCreatedAtDesc();
        return orders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<OrderDTO> getOrdersByStatus(Order.OrderStatus status) {
        List<Order> orders = orderRepository.findByOrderStatus(status);
        return orders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<OrderDTO> getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .map(this::convertToDTO);
    }

    public Optional<OrderDTO> getOrderByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .map(this::convertToDTO);
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, Order.OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        order.setOrderStatus(newStatus);

        // Update actual delivery time if done
        if (newStatus == Order.OrderStatus.DONE) {
            order.setActualDeliveryTime(LocalDateTime.now());

            // Tự động set payment status = COMPLETED cho đơn CASH khi DONE
            if (order.getPaymentMethod() == Order.PaymentMethod.CASH) {
                order.setPaymentStatus(Order.PaymentStatus.COMPLETED);

                // Cập nhật Payment entity
                List<Payment> payments = paymentRepository.findByOrderOrderId(order.getOrderId());
                if (!payments.isEmpty()) {
                    Payment payment = payments.get(0);
                    payment.setPaymentStatus(Payment.PaymentStatus.COMPLETED);
                    payment.setPaymentDate(LocalDateTime.now());
                    payment.setPaymentNotes("Thanh toán tiền mặt khi giao hàng hoàn tất");
                    paymentRepository.save(payment);
                }
                log.info("Auto-completed payment status for CASH order: {}", orderId);
            }
        } else if (newStatus == Order.OrderStatus.CANCELLED) {
            // Khi hủy đơn, cập nhật trạng thái thanh toán về FAILED
            order.setPaymentStatus(Order.PaymentStatus.FAILED);
            List<Payment> payments = paymentRepository.findByOrderOrderId(order.getOrderId());
            if (!payments.isEmpty()) {
                Payment payment = payments.get(0);
                payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
                payment.setPaymentDate(LocalDateTime.now());
                payment.setPaymentNotes("Đơn hàng đã bị hủy - thanh toán thất bại");
                paymentRepository.save(payment);
            }
            log.info("Payment marked FAILED due to order cancellation: {}", orderId);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order status updated: {} -> {}", orderId, newStatus);

        // Gửi notification cho user về cập nhật trạng thái
        if (notificationService != null) {
            try {
                Long userId = savedOrder.getUser().getUserId();
                notificationService.notifyOrderStatusUpdate(
                        userId,
                        orderId,
                        savedOrder.getOrderNumber(),
                        newStatus
                );
            } catch (Exception e) {
                log.error("Error sending order status update notification: {}", e.getMessage(), e);
            }
        }

        return convertToDTO(savedOrder);
    }

    @Transactional
    public OrderDTO cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.canBeCancelled()) {
            throw new IllegalStateException("Order cannot be cancelled");
        }

        // Set order status to CANCELLED
        order.setOrderStatus(Order.OrderStatus.CANCELLED);
        // Also set payment status to FAILED
        order.setPaymentStatus(Order.PaymentStatus.FAILED);
        // Update related payment record if exists
        List<Payment> payments = paymentRepository.findByOrderOrderId(order.getOrderId());
        if (!payments.isEmpty()) {
            Payment payment = payments.get(0);
            payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
            payment.setPaymentDate(LocalDateTime.now());
            payment.setPaymentNotes("Đơn hàng đã bị hủy - thanh toán thất bại");
            paymentRepository.save(payment);
        }
        Order savedOrder = orderRepository.save(order);

        log.info("Order cancelled: {}", orderId);
        return convertToDTO(savedOrder);
    }

    public OrderStatsDTO getOrderStats() {
        // Get counts by status
        Long totalOrders = orderRepository.count();
        Long pendingOrders = orderRepository.countByOrderStatus(Order.OrderStatus.PENDING);
        Long confirmedOrders = orderRepository.countByOrderStatus(Order.OrderStatus.CONFIRMED);
        Long deliveringOrders = orderRepository.countByOrderStatus(Order.OrderStatus.DELIVERING);
        Long completedOrders = orderRepository.countByOrderStatus(Order.OrderStatus.DONE);

        // Calculate revenue (simplified - you can enhance this)
        BigDecimal totalRevenue = BigDecimal.ZERO; // TODO: Calculate from orders
        BigDecimal todayRevenue = BigDecimal.ZERO; // TODO: Calculate from today's orders
        BigDecimal thisMonthRevenue = BigDecimal.ZERO; // TODO: Calculate from this month's orders

        return OrderStatsDTO.builder()
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .confirmedOrders(confirmedOrders)
                .deliveringOrders(deliveringOrders)
                .completedOrders(completedOrders)
                .totalRevenue(totalRevenue)
                .todayRevenue(todayRevenue)
                .thisMonthRevenue(thisMonthRevenue)
                .build();
    }

    // ===============================
    // DASHBOARD ANALYTICS METHODS
    // ===============================

    /**
     * Lấy số lượng order theo khoảng thời gian
     */
    public long getOrderCountByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        return orderRepository.countByCreatedAtBetween(startDateTime, endDateTime);
    }

    /**
     * Lấy tổng doanh thu theo khoảng thời gian
     */
    public double getRevenueByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<Order> orders = orderRepository.findByCreatedAtBetweenAndOrderStatus(
                startDateTime, endDateTime, Order.OrderStatus.DONE);

        return orders.stream()
                .mapToDouble(order -> order.getFinalAmount().doubleValue())
                .sum();
    }

    /**
     * Lấy thống kê order theo trạng thái trong khoảng thời gian
     */
    public Map<Order.OrderStatus, Long> getOrderCountByStatusInDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        Map<Order.OrderStatus, Long> statusCounts = new java.util.HashMap<>();

        for (Order.OrderStatus status : Order.OrderStatus.values()) {
            long count = orderRepository.countByCreatedAtBetweenAndOrderStatus(startDateTime, endDateTime, status);
            statusCounts.put(status, count);
        }

        return statusCounts;
    }

    // ===============================
    // HELPER METHODS
    // ===============================

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private OrderDTO convertToDTO(Order order) {
        return OrderDTO.builder()
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser().getUserId())
                .userFullName(order.getUser().getFullName())
                .userEmail(order.getUser().getEmail())
                .userPhone(order.getUser().getPhoneNumber())
                .orderStatus(order.getOrderStatus())
                .totalAmount(order.getTotalAmount())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .couponDiscount(order.getCouponDiscount())
                .finalAmount(order.getFinalAmount())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryNotes(order.getDeliveryNotes())
                .estimatedDeliveryTime(order.getEstimatedDeliveryTime())
                .actualDeliveryTime(order.getActualDeliveryTime())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .orderItems(order.getOrderItems() != null ?
                        order.getOrderItems().stream()
                                .map(this::convertOrderItemToDTO)
                                .collect(Collectors.toList()) : null)
                .build();
    }

    private OrderItemDTO convertOrderItemToDTO(OrderItem orderItem) {
        return OrderItemDTO.builder()
                .orderItemId(orderItem.getOrderItemId())
                .orderId(orderItem.getOrder().getOrderId())
                .productId(orderItem.getProduct().getProductId())
                .productName(orderItem.getProduct().getName())
                .productImageUrl(orderItem.getProduct().getImageUrl())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .salePrice(orderItem.getSalePrice())
                .totalPrice(orderItem.getTotalPrice())
                .specialInstructions(orderItem.getSpecialInstructions())
                .orderItemOptions(orderItem.getOrderItemOptions() != null ?
                        orderItem.getOrderItemOptions().stream()
                                .map(this::convertOrderItemOptionToDTO)
                                .collect(Collectors.toList()) : null)
                .createdAt(orderItem.getCreatedAt())
                .build();
    }

    private OrderItemOptionDTO convertOrderItemOptionToDTO(OrderItemOption option) {
        return OrderItemOptionDTO.builder()
                .orderItemOptionId(option.getOrderItemOptionId())
                .orderItemId(option.getOrderItem().getOrderItemId())
                .optionId(option.getProductOption().getOptionId())
                .optionName(option.getOptionName())
                .optionType(option.getOptionType().toString())
                .price(option.getPrice())
                .createdAt(option.getCreatedAt())
                .build();
    }

    private BigDecimal calculateTotalAmount(List<CreateOrderRequest.OrderItemRequest> orderItems) {
        return orderItems.stream()
                .map(itemRequest -> {
                    Product product = productRepository.findById(itemRequest.getProductId())
                            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemRequest.getProductId()));

                    // Use original unit price (giá gốc) for coupon base subtotal
                    BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

                    // Add option prices
                    if (itemRequest.getSelectedOptionIds() != null) {
                        BigDecimal optionsPrice = itemRequest.getSelectedOptionIds().stream()
                                .map(optionId -> {
                                    ProductOption option = productOptionRepository.findById(optionId)
                                            .orElseThrow(() -> new IllegalArgumentException("Product option not found: " + optionId));
                                    return option.getPrice();
                                })
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        // Options áp dụng cho mỗi đơn vị sản phẩm
                        itemTotal = itemTotal.add(optionsPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
                    }

                    return itemTotal;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Public helpers for quoting from controller
    public BigDecimal getOrderItemsSubtotal(List<CreateOrderRequest.OrderItemRequest> orderItems) {
        return calculateTotalAmount(orderItems);
    }

    public BigDecimal previewCouponDiscount(String couponCode, BigDecimal subtotal) {
        java.util.Optional<Coupon> c = couponRepository.findValidCoupon(couponCode, java.time.LocalDateTime.now());
        return c.map(coupon -> coupon.calculateDiscount(subtotal)).orElse(BigDecimal.ZERO);
    }
}