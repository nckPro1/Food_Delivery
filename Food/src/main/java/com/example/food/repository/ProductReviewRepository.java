package com.example.food.repository;

import com.example.food.model.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    Page<ProductReview> findByProductIdAndIsVisibleTrueOrderByCreatedAtDesc(Long productId, Pageable pageable);

    // Query để lấy reviews có isVisible = true hoặc null (cho các reviews cũ chưa set isVisible)
    @Query("SELECT r FROM ProductReview r WHERE r.productId = :productId " +
            "AND (r.isVisible = true OR r.isVisible IS NULL) " +
            "ORDER BY r.createdAt DESC")
    Page<ProductReview> findByProductIdAndIsVisibleTrueOrNullOrderByCreatedAtDesc(@Param("productId") Long productId, Pageable pageable);

    Optional<ProductReview> findFirstByProductIdAndUserIdOrderByCreatedAtDesc(Long productId, Long userId);
    Optional<ProductReview> findByOrderItemId(Long orderItemId);
    long countByProductIdAndIsVisibleTrue(Long productId);
}


