package com.example.food.repository;

import com.example.food.model.ProductReviewComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductReviewCommentRepository extends JpaRepository<ProductReviewComment, Long> {
    Page<ProductReviewComment> findByProductIdAndIsVisibleTrueOrderByCreatedAtDesc(Long productId, Pageable pageable);
}


