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
import java.util.List;
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
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ShippingFeeService shippingFeeService;
    private final EnhancedShippingFeeService enhancedShippingFeeService;

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

        // Create order
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .orderStatus(Order.OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .paymentMethod(Order.PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()))
                .deliveryAddress(deliveryAddress) // Use delivery address from request or user's address
                .deliveryNotes(request.getDeliveryNotes())
                .deliveryCity(request.getDeliveryCity())
                .deliveryDistrict(request.getDeliveryDistrict())
                .deliveryWard(request.getDeliveryWard())
                .deliveryStreet(request.getDeliveryStreet())
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

        // Calculate shipping fee
        BigDecimal shippingFee;
        Integer estimatedDuration = 30; // Default 30 minutes

        if (request.getDeliveryCity() != null && request.getDeliveryDistrict() != null) {
            // Use area-based calculation
            ShippingCalculationResponse shippingResponse = enhancedShippingFeeService
                    .calculateShippingFee(totalAmount, request.getDeliveryCity(), request.getDeliveryDistrict());

            shippingFee = shippingResponse.getShippingFee();
            estimatedDuration = shippingResponse.getEstimatedDurationMinutes();

        } else {
            // Fallback to original calculation
            shippingFee = shippingFeeService.calculateShippingFee(totalAmount);
        }

        order.setShippingFee(shippingFee);

        // Calculate final amount
        order.calculateFinalAmount();

        // Set estimated delivery time
        order.setEstimatedDeliveryTime(LocalDateTime.now().plusMinutes(estimatedDuration));

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

        // Convert to DTO for notification
        OrderDTO orderDTO = convertToDTO(finalSavedOrder);

        // Send notification to admin
        notificationService.notifyNewOrder(orderDTO);

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

        // Since we removed CANCELLED status, we'll mark as DONE with cancellation note
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
}