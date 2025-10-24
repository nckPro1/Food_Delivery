package com.example.food.controller;

import com.example.food.dto.ApiResponse;
import com.example.food.model.Product;
import com.example.food.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products/images")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProductImageController {

    private final ProductService productService;

    /**
     * Upload ảnh chính cho sản phẩm
     */
    @PostMapping("/{productId}/main")
    public ResponseEntity<ApiResponse<String>> uploadMainImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                        .success(false)
                        .message("File không được để trống")
                        .build());
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                        .success(false)
                        .message("Chỉ chấp nhận file ảnh")
                        .build());
            }

            // Validate file size (10MB)
            long maxSize = 10 * 1024 * 1024; // 10MB
            if (file.getSize() > maxSize) {
                return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                        .success(false)
                        .message("File quá lớn. Kích thước tối đa là 10MB")
                        .build());
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ?
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String filename = "product_" + productId + "_main_" + UUID.randomUUID().toString() + extension;

            // Create upload directory if not exists
            Path uploadDir = Paths.get("uploads/products");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Save file
            Path filePath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Update product main image URL
            String imageUrl = "/uploads/products/" + filename;
            Product product = productService.getProductById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));

            product.setImageUrl(imageUrl);
            productService.updateProduct(productId, product);

            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true)
                    .message("Upload ảnh chính thành công")
                    .data(imageUrl)
                    .build());

        } catch (IOException e) {
            log.error("Error uploading main image for product {}: ", productId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .success(false)
                    .message("Lỗi upload file: " + e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error uploading main image for product {}: ", productId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .success(false)
                    .message("Lỗi server: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Upload nhiều ảnh cho gallery sản phẩm
     */
    @PostMapping("/{productId}/gallery")
    public ResponseEntity<ApiResponse<List<String>>> uploadGalleryImages(
            @PathVariable Long productId,
            @RequestParam("files") MultipartFile[] files) {

        try {
            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest().body(ApiResponse.<List<String>>builder()
                        .success(false)
                        .message("Không có file nào được upload")
                        .build());
            }

            // Validate number of files (max 10 images)
            if (files.length > 10) {
                return ResponseEntity.badRequest().body(ApiResponse.<List<String>>builder()
                        .success(false)
                        .message("Tối đa 10 ảnh cho gallery")
                        .build());
            }

            List<String> uploadedUrls = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            // Create upload directory if not exists
            Path uploadDir = Paths.get("uploads/products");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            for (MultipartFile file : files) {
                try {
                    // Validate file
                    if (file.isEmpty()) {
                        errors.add("File " + file.getOriginalFilename() + " trống");
                        continue;
                    }

                    String contentType = file.getContentType();
                    if (contentType == null || !contentType.startsWith("image/")) {
                        errors.add("File " + file.getOriginalFilename() + " không phải ảnh");
                        continue;
                    }

                    long maxSize = 10 * 1024 * 1024; // 10MB
                    if (file.getSize() > maxSize) {
                        errors.add("File " + file.getOriginalFilename() + " quá lớn (>10MB)");
                        continue;
                    }

                    // Generate unique filename
                    String originalFilename = file.getOriginalFilename();
                    String extension = originalFilename != null ?
                            originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
                    String filename = "product_" + productId + "_gallery_" + UUID.randomUUID().toString() + extension;

                    // Save file
                    Path filePath = uploadDir.resolve(filename);
                    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                    String imageUrl = "/uploads/products/" + filename;
                    uploadedUrls.add(imageUrl);

                } catch (IOException e) {
                    errors.add("Lỗi upload file " + file.getOriginalFilename() + ": " + e.getMessage());
                }
            }

            if (uploadedUrls.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.<List<String>>builder()
                        .success(false)
                        .message("Không có file nào upload thành công. Lỗi: " + String.join(", ", errors))
                        .build());
            }

            // Update product gallery URLs
            Product product = productService.getProductById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));

            // Merge with existing gallery URLs
            List<String> existingUrls = product.getGalleryUrlsList();
            existingUrls.addAll(uploadedUrls);
            product.setGalleryUrlsList(existingUrls);

            productService.updateProduct(productId, product);

            String message = "Upload thành công " + uploadedUrls.size() + " ảnh";
            if (!errors.isEmpty()) {
                message += ". Lỗi: " + String.join(", ", errors);
            }

            return ResponseEntity.ok(ApiResponse.<List<String>>builder()
                    .success(true)
                    .message(message)
                    .data(uploadedUrls)
                    .build());

        } catch (Exception e) {
            log.error("Error uploading gallery images for product {}: ", productId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<List<String>>builder()
                    .success(false)
                    .message("Lỗi server: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Xóa ảnh khỏi gallery
     */
    @DeleteMapping("/{productId}/gallery")
    public ResponseEntity<ApiResponse<Void>> removeGalleryImage(
            @PathVariable Long productId,
            @RequestParam String imageUrl) {

        try {
            Product product = productService.getProductById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));

            List<String> galleryUrls = product.getGalleryUrlsList();
            boolean removed = galleryUrls.remove(imageUrl);

            if (!removed) {
                return ResponseEntity.badRequest().body(ApiResponse.<Void>builder()
                        .success(false)
                        .message("Ảnh không tồn tại trong gallery")
                        .build());
            }

            product.setGalleryUrlsList(galleryUrls);
            productService.updateProduct(productId, product);

            // Optionally delete the physical file
            try {
                String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                Path filePath = Paths.get("uploads/products/" + filename);
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.warn("Could not delete physical file: {}", imageUrl);
            }

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Xóa ảnh khỏi gallery thành công")
                    .build());

        } catch (Exception e) {
            log.error("Error removing gallery image for product {}: ", productId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Lỗi server: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Xóa tất cả ảnh gallery
     */
    @DeleteMapping("/{productId}/gallery/all")
    public ResponseEntity<ApiResponse<Void>> clearGalleryImages(@PathVariable Long productId) {
        try {
            Product product = productService.getProductById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));

            List<String> galleryUrls = product.getGalleryUrlsList();

            // Delete physical files
            for (String imageUrl : galleryUrls) {
                try {
                    String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                    Path filePath = Paths.get("uploads/products/" + filename);
                    Files.deleteIfExists(filePath);
                } catch (IOException e) {
                    log.warn("Could not delete physical file: {}", imageUrl);
                }
            }

            // Clear gallery URLs
            product.setGalleryUrlsList(new ArrayList<>());
            productService.updateProduct(productId, product);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Xóa tất cả ảnh gallery thành công")
                    .build());

        } catch (Exception e) {
            log.error("Error clearing gallery images for product {}: ", productId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Lỗi server: " + e.getMessage())
                    .build());
        }
    }
}
