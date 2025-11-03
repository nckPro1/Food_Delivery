package com.example.food.repository;

import com.example.food.model.ProductReviewComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductReviewCommentRepository extends JpaRepository<ProductReviewComment, Long> {
    Page<ProductReviewComment> findByProductIdAndIsVisibleTrueOrderByCreatedAtDesc(Long productId, Pageable pageable);

    // Query để lấy comments có isVisible = true hoặc null (cho các comments cũ chưa set isVisible)
    @Query("SELECT c FROM ProductReviewComment c WHERE c.productId = :productId " +
            "AND (c.isVisible = true OR c.isVisible IS NULL) " +
            "ORDER BY c.createdAt DESC")
    Page<ProductReviewComment> findByProductIdAndIsVisibleTrueOrNullOrderByCreatedAtDesc(@Param("productId") Long productId, Pageable pageable);
}


