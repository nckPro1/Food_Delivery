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
import java.util.List;

@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "image_url", length = 500)
    private String imageUrl; // Ảnh chính

    @Column(name = "gallery_urls", columnDefinition = "JSON")
    private String galleryUrls; // Nhiều ảnh dưới dạng JSON

    @Column(name = "has_options", nullable = false)
    @Builder.Default
    private Boolean hasOptions = false;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "preparation_time")
    @Builder.Default
    private Integer preparationTime = 15; // Phút

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @ToString.Exclude
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<ProductOption> options;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Helper methods for gallery URLs
    public List<String> getGalleryUrlsList() {
        if (galleryUrls == null || galleryUrls.isEmpty()) {
            return List.of();
        }
        try {
            return List.of(galleryUrls.split(","));
        } catch (Exception e) {
            return List.of();
        }
    }

    public void setGalleryUrlsList(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            this.galleryUrls = null;
        } else {
            this.galleryUrls = String.join(",", urls);
        }
    }

    // Helper method to get all images (main + gallery)
    public List<String> getAllImages() {
        List<String> allImages = getGalleryUrlsList();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            allImages.add(0, imageUrl); // Add main image at the beginning
        }
        return allImages;
    }
}
