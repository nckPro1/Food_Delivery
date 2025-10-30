package com.example.food.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_item_options")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_option_id")
    private Long orderItemOptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private ProductOption productOption;

    @Column(name = "option_name", nullable = false, length = 100)
    private String optionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", nullable = false)
    private ProductOption.OptionType optionType;

    @Column(name = "price", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
