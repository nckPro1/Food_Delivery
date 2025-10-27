package com.example.food.service;

import com.example.food.dto.*;
import com.example.food.model.*;
import com.example.food.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        // Process coupon if provided
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            try {
                // Calculate total amount first to validate coupon
                BigDecimal tempTotal = calculateTotalAmount(request.getOrderItems());
                Coupon coupon = couponRepository.findValidCoupon(request.getCouponCode(), LocalDateTime.now())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid or expired coupon"));

                // Calculate actual discount using the coupon's calculateDiscount method
                couponDiscount = coupon.calculateDiscount(tempTotal);

                log.info("Applied coupon: {}, discount: {}", request.getCouponCode(), couponDiscount);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid coupon: " + e.getMessage());
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
                                            .extraPrice(productOption.getPrice())
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
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order status updated: {} -> {}", orderId, newStatus);

        return convertToDTO(savedOrder);
    }

    @Transactional
    public OrderDTO cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.canBeCancelled()) {
            throw new IllegalStateException("Order cannot be cancelled");
        }

        // Since we don't have CANCELLED status, we'll mark as DONE with cancellation note
        order.setOrderStatus(Order.OrderStatus.DONE);
        order.setDeliveryNotes(order.getDeliveryNotes() + " [ĐÃ HỦY]");
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
                .optionName(option.getProductOption().getOptionName())
                .optionType(option.getProductOption().getOptionType().toString())
                .extraPrice(option.getExtraPrice())
                .build();
    }

    private BigDecimal calculateTotalAmount(List<CreateOrderRequest.OrderItemRequest> orderItems) {
        return orderItems.stream()
                .map(itemRequest -> {
                    Product product = productRepository.findById(itemRequest.getProductId())
                            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemRequest.getProductId()));

                    BigDecimal itemTotal = product.getCurrentPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

                    // Add option prices
                    if (itemRequest.getSelectedOptionIds() != null) {
                        BigDecimal optionsPrice = itemRequest.getSelectedOptionIds().stream()
                                .map(optionId -> {
                                    ProductOption option = productOptionRepository.findById(optionId)
                                            .orElseThrow(() -> new IllegalArgumentException("Product option not found: " + optionId));
                                    return option.getPrice();
                                })
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        itemTotal = itemTotal.add(optionsPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
                    }

                    return itemTotal;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}