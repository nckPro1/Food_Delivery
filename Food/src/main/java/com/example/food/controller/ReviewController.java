package com.example.food.controller;

import com.example.food.dto.ApiResponse;
import com.example.food.dto.CreateCommentRequest;
import com.example.food.dto.CreateOrUpdateReviewRequest;
import com.example.food.dto.ReviewDTO;
import com.example.food.model.ProductReview;
import com.example.food.model.ProductReviewComment;
import com.example.food.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ReviewController {

    private final ReviewService reviewService;

    // ===============================
    // REVIEWS
    // ===============================

    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<Page<ReviewDTO>>> listReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductReview> reviews = reviewService.listReviews(productId, pageable);
            Page<ReviewDTO> dtoPage = reviews.map(this::toDTO);
            return ResponseEntity.ok(ApiResponse.<Page<ReviewDTO>>builder()
                    .success(true)
                    .message("Reviews retrieved")
                    .data(dtoPage)
                    .build());
        } catch (Exception e) {
            log.error("Error listReviews productId={}", productId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<Page<ReviewDTO>>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    // Create or update a review bound to an orderItemId
    @PostMapping("/products/{productId}/reviews/{orderItemId}")
    public ResponseEntity<ApiResponse<ReviewDTO>> createOrUpdateReview(
            @PathVariable Long productId,
            @PathVariable Long orderItemId,
            @RequestParam Long userId,
            @RequestBody CreateOrUpdateReviewRequest request) {
        try {
            ProductReview saved = reviewService.createOrUpdateReview(
                    productId,
                    userId,
                    orderItemId,
                    request.getRating(),
                    request.getComment(),
                    request.getImageUrls()
            );
            return ResponseEntity.ok(ApiResponse.<ReviewDTO>builder()
                    .success(true)
                    .message("Review saved")
                    .data(toDTO(saved))
                    .build());
        } catch (Exception e) {
            log.error("Error createOrUpdateReview productId={}, orderItemId={}", productId, orderItemId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<ReviewDTO>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @GetMapping("/products/{productId}/reviews/me")
    public ResponseEntity<ApiResponse<ReviewDTO>> getMyLatestReview(
            @PathVariable Long productId,
            @RequestParam Long userId) {
        try {
            return reviewService
                    .listReviews(productId, PageRequest.of(0, 1))
                    .stream()
                    .findFirst()
                    .map(r -> ResponseEntity.ok(ApiResponse.<ReviewDTO>builder()
                            .success(true)
                            .message("OK")
                            .data(toDTO(r))
                            .build()))
                    .orElseGet(() -> ResponseEntity.ok(ApiResponse.<ReviewDTO>builder()
                            .success(true)
                            .message("No review")
                            .data(null)
                            .build()));
        } catch (Exception e) {
            log.error("Error getMyLatestReview productId={}", productId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<ReviewDTO>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    // ===============================
    // COMMENTS
    // ===============================

    @GetMapping("/products/{productId}/comments")
    public ResponseEntity<ApiResponse<Page<String>>> listComments(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductReviewComment> comments = reviewService.listComments(productId, pageable);
            // Keep simple: return raw JSON strings combined; in real app create a proper DTO
            Page<String> data = comments.map(c -> c.getContent());
            return ResponseEntity.ok(ApiResponse.<Page<String>>builder()
                    .success(true)
                    .message("Comments retrieved")
                    .data(data)
                    .build());
        } catch (Exception e) {
            log.error("Error listComments productId={}", productId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<Page<String>>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @PostMapping("/products/{productId}/comments")
    public ResponseEntity<ApiResponse<Long>> createComment(
            @PathVariable Long productId,
            @RequestParam Long userId,
            @RequestBody CreateCommentRequest request) {
        try {
            ProductReviewComment saved = reviewService.createComment(
                    productId,
                    userId,
                    request.getReviewId(),
                    request.getContent(),
                    request.getAttachmentUrls()
            );
            return ResponseEntity.ok(ApiResponse.<Long>builder()
                    .success(true)
                    .message("Comment created")
                    .data(saved.getCommentId())
                    .build());
        } catch (Exception e) {
            log.error("Error createComment productId={}", productId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<Long>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    private ReviewDTO toDTO(ProductReview r) {
        List<String> images = null;
        if (r.getImageUrls() != null && !r.getImageUrls().isEmpty()) {
            images = Arrays.stream(r.getImageUrls().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        return ReviewDTO.builder()
                .reviewId(r.getReviewId())
                .productId(r.getProductId())
                .userId(r.getUserId())
                .orderItemId(r.getOrderItemId())
                .rating(r.getRating())
                .comment(r.getComment())
                .imageUrls(images)
                .imageCount(r.getImageCount())
                .isVerifiedPurchase(r.getIsVerifiedPurchase())
                .helpfulCount(r.getHelpfulCount())
                .isVisible(r.getIsVisible())
                .adminReply(r.getAdminReply())
                .adminRepliedAt(r.getAdminRepliedAt())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}


