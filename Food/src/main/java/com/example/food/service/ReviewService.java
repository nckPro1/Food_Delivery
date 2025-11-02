package com.example.food.service;

import com.example.food.model.Product;
import com.example.food.model.ProductReview;
import com.example.food.model.ProductReviewComment;
import com.example.food.model.User;
import com.example.food.repository.ProductRepository;
import com.example.food.repository.ProductReviewCommentRepository;
import com.example.food.repository.ProductReviewRepository;
import com.example.food.repository.UserRepository;
import com.example.food.repository.OrderItemRepository;
import com.example.food.dto.ReviewActivityDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReviewService {

    private final ProductReviewRepository productReviewRepository;
    private final ProductReviewCommentRepository productReviewCommentRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    // Reviews
    @Transactional(readOnly = true)
    public Page<ProductReview> listReviews(Long productId, Pageable pageable) {
        return productReviewRepository.findByProductIdAndIsVisibleTrueOrderByCreatedAtDesc(productId, pageable);
    }

    public ProductReview createOrUpdateReview(Long productId,
                                              Long userId,
                                              Long orderItemId,
                                              Integer rating,
                                              String comment,
                                              List<String> imageUrls) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        // Validate purchased condition: order item must belong to user, DONE, and match product
        var orderItemOpt = orderItemRepository.findById(orderItemId);
        if (orderItemOpt.isEmpty()) {
            throw new IllegalArgumentException("Order item not found");
        }
        var orderItem = orderItemOpt.get();
        if (orderItem.getOrder() == null || orderItem.getOrder().getUser() == null) {
            throw new IllegalArgumentException("Invalid order item data");
        }
        if (!orderItem.getOrder().getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Bạn chỉ có thể đánh giá các đơn hàng của chính bạn");
        }
        if (orderItem.getProduct() == null || !orderItem.getProduct().getProductId().equals(productId)) {
            throw new IllegalArgumentException("Order item không thuộc sản phẩm này");
        }
        if (orderItem.getOrder().getOrderStatus() != com.example.food.model.Order.OrderStatus.DONE) {
            throw new IllegalArgumentException("Chỉ được đánh giá sau khi đơn hàng hoàn thành");
        }

        Optional<ProductReview> existing = productReviewRepository.findByOrderItemId(orderItemId);
        ProductReview review = existing.orElseGet(() -> ProductReview.builder()
                .productId(productId)
                .userId(userId)
                .orderItemId(orderItemId)
                .build());

        review.setRating(rating);
        review.setComment(comment);
        if (imageUrls != null && !imageUrls.isEmpty()) {
            review.setImageUrls(String.join(",", imageUrls));
            review.setImageCount(imageUrls.size());
        } else {
            review.setImageUrls(null);
            review.setImageCount(0);
        }

        ProductReview saved = productReviewRepository.save(review);
        recalcProductAggregates(productId);
        return saved;
    }

    // Comments
    @Transactional(readOnly = true)
    public Page<ProductReviewComment> listComments(Long productId, Pageable pageable) {
        return productReviewCommentRepository.findByProductIdAndIsVisibleTrueOrderByCreatedAtDesc(productId, pageable);
    }

    public ProductReviewComment createComment(Long productId,
                                              Long userId,
                                              Long maybeReviewId,
                                              String content,
                                              List<String> attachments) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content is required");
        }

        ProductReviewComment comment = ProductReviewComment.builder()
                .productId(productId)
                .userId(userId)
                .reviewId(maybeReviewId)
                .content(content.trim())
                .build();

        if (attachments != null && !attachments.isEmpty()) {
            comment.setAttachmentUrls(String.join(",", attachments));
        }

        return productReviewCommentRepository.save(comment);
    }

    // Aggregates
    private void recalcProductAggregates(Long productId) {
        List<ProductReview> visibleReviews = productReviewRepository
                .findByProductIdAndIsVisibleTrueOrderByCreatedAtDesc(productId, Pageable.unpaged())
                .getContent();
        int count = visibleReviews.size();
        double avg = 0.0;
        if (count > 0) {
            avg = visibleReviews.stream().mapToInt(ProductReview::getRating).average().orElse(0.0);
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        product.setPrice(product.getPrice()); // no-op to touch entity
        product.setIsAvailable(product.getIsAvailable());
        // reuse existing fields; set new ones
        try {
            // If fields exist in entity, set via reflection-safe approach
            var cls = product.getClass();
            try {
                var avgField = cls.getDeclaredField("averageRating");
                avgField.setAccessible(true);
                avgField.set(product, BigDecimal.valueOf(Math.round(avg * 100.0) / 100.0));
            } catch (NoSuchFieldException ignore) {}
            try {
                var countField = cls.getDeclaredField("totalReviews");
                countField.setAccessible(true);
                countField.set(product, count);
            } catch (NoSuchFieldException ignore) {}
        } catch (Exception e) {
            log.warn("Could not update product aggregates on entity, ensure fields exist: {}", e.getMessage());
        }
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<ReviewActivityDTO> getRecentActivities(int size) {
        var reviews = productReviewRepository
                .findByProductIdAndIsVisibleTrueOrderByCreatedAtDesc(null, Pageable.ofSize(size)); // placeholder
        // Fallback: get latest globally (add repository if needed). We'll fetch all and limit.
        List<ProductReview> latestReviews = productReviewRepository.findAll(Pageable.ofSize(size)).getContent();
        List<ProductReviewComment> latestComments = productReviewCommentRepository.findAll(Pageable.ofSize(size)).getContent();

        List<ReviewActivityDTO> items = new java.util.ArrayList<>();
        for (ProductReview r : latestReviews) {
            items.add(ReviewActivityDTO.builder()
                    .type("REVIEW")
                    .id(r.getReviewId())
                    .productId(r.getProductId())
                    .productName(resolveProductName(r.getProductId()))
                    .userId(r.getUserId())
                    .userName(resolveUserName(r.getUserId()))
                    .rating(r.getRating())
                    .content(r.getComment())
                    .createdAt(r.getCreatedAt())
                    .build());
        }
        for (ProductReviewComment c : latestComments) {
            items.add(ReviewActivityDTO.builder()
                    .type("COMMENT")
                    .id(c.getCommentId())
                    .productId(c.getProductId())
                    .productName(resolveProductName(c.getProductId()))
                    .userId(c.getUserId())
                    .userName(resolveUserName(c.getUserId()))
                    .content(c.getContent())
                    .createdAt(c.getCreatedAt())
                    .build());
        }
        items.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return items.size() > size ? items.subList(0, size) : items;
    }

    private String resolveProductName(Long productId) {
        try {
            return productRepository.findById(productId).map(Product::getName).orElse("#" + productId);
        } catch (Exception e) {
            return "#" + productId;
        }
    }

    private String resolveUserName(Long userId) {
        try {
            return userRepository.findById(userId).map(User::getFullName).orElse("User " + userId);
        } catch (Exception e) {
            return "User " + userId;
        }
    }
}


