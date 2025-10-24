package com.example.food.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_options")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    private Long optionId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "option_name", nullable = false, length = 100)
    private String optionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", nullable = false)
    private OptionType optionType;

    @Column(name = "price", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean isRequired = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "max_selections")
    @Builder.Default
    private Integer maxSelections = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    @ToString.Exclude
    private Product product;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum OptionType {
        SIZE("SIZE"),
        TOPPING("TOPPING"),
        EXTRA("EXTRA"),
        CUSTOMIZATION("CUSTOMIZATION");

        private final String value;

        OptionType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
