package com.example.food.controller;

import com.example.food.dto.*;
import com.example.food.model.Product;
import com.example.food.model.Category;
import com.example.food.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;

    // ===============================
    // PUBLIC PRODUCT ENDPOINTS
    // ===============================

    /**
     * Lấy tất cả sản phẩm có sẵn với pagination
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductDTO>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ?
                    Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<Product> products = productService.getAllAvailableProducts(pageable);
            Page<ProductDTO> productDTOs = products.map(this::convertToDTO);

            return ResponseEntity.ok(ApiResponse.<Page<ProductDTO>>builder()
                    .success(true)
                    .message("Products retrieved successfully")
                    .data(productDTOs)
                    .build());
        } catch (Exception e) {
            log.error("Error retrieving products: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Page<ProductDTO>>builder()
                    .success(false)
                    .message("Error retrieving products: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Lấy sản phẩm theo ID
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDTO>> getProductById(@PathVariable Long productId) {
        try {
            Product product = productService.getProductById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));

            // Chỉ cho phép truy cập sản phẩm đang hoạt động ở API public
            if (Boolean.FALSE.equals(product.getIsAvailable())) {
                return ResponseEntity.status(404).body(ApiResponse.<ProductDTO>builder()
                        .success(false)
                        .message("Product not found")
                        .build());
            }

            return ResponseEntity.ok(ApiResponse.<ProductDTO>builder()
                    .success(true)
                    .message("Product retrieved successfully")
                    .data(convertToDTO(product))
                    .build());
        } catch (Exception e) {
            log.error("Error retrieving product {}: ", productId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<ProductDTO>builder()
                    .success(false)
                    .message("Error retrieving product: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Lấy sản phẩm theo category
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getProductsByCategory(@PathVariable Long categoryId) {
        try {
            List<Product> products = productService.getProductsByCategory(categoryId);
            List<ProductDTO> productDTOs = products.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.<List<ProductDTO>>builder()
                    .success(true)
                    .message("Products retrieved successfully")
                    .data(productDTOs)
                    .build());
        } catch (Exception e) {
            log.error("Error retrieving products by category {}: ", categoryId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<List<ProductDTO>>builder()
                    .success(false)
                    .message("Error retrieving products: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Lấy sản phẩm featured
     */
    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getFeaturedProducts() {
        try {
            List<Product> products = productService.getFeaturedProducts();
            List<ProductDTO> productDTOs = products.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.<List<ProductDTO>>builder()
                    .success(true)
                    .message("Featured products retrieved successfully")
                    .data(productDTOs)
                    .build());
        } catch (Exception e) {
            log.error("Error retrieving featured products: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<List<ProductDTO>>builder()
                    .success(false)
                    .message("Error retrieving featured products: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Tìm kiếm sản phẩm theo tên
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductDTO>>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Product> products = productService.searchProductsByName(keyword, pageable);
            Page<ProductDTO> productDTOs = products.map(this::convertToDTO);

            return ResponseEntity.ok(ApiResponse.<Page<ProductDTO>>builder()
                    .success(true)
                    .message("Search results retrieved successfully")
                    .data(productDTOs)
                    .build());
        } catch (Exception e) {
            log.error("Error searching products: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Page<ProductDTO>>builder()
                    .success(false)
                    .message("Error searching products: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Lấy sản phẩm có nhiều ảnh
     */
    @GetMapping("/with-gallery")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getProductsWithGallery() {
        try {
            List<Product> products = productService.getProductsWithGallery();
            List<ProductDTO> productDTOs = products.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.<List<ProductDTO>>builder()
                    .success(true)
                    .message("Products with gallery retrieved successfully")
                    .data(productDTOs)
                    .build());
        } catch (Exception e) {
            log.error("Error retrieving products with gallery: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<List<ProductDTO>>builder()
                    .success(false)
                    .message("Error retrieving products with gallery: " + e.getMessage())
                    .build());
        }
    }

    // ===============================
    // ADMIN ENDPOINTS (Protected)
    // ===============================

    /**
     * Tạo sản phẩm mới (Admin only)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductDTO>> createProduct(@RequestBody CreateProductRequest request) {
        try {
            Product product = convertToEntity(request);
            Product createdProduct = productService.createProduct(product);

            return ResponseEntity.ok(ApiResponse.<ProductDTO>builder()
                    .success(true)
                    .message("Product created successfully")
                    .data(convertToDTO(createdProduct))
                    .build());
        } catch (Exception e) {
            log.error("Error creating product: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<ProductDTO>builder()
                    .success(false)
                    .message("Error creating product: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Cập nhật sản phẩm (Admin only)
     */
    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDTO>> updateProduct(
            @PathVariable Long productId,
            @RequestBody UpdateProductRequest request) {
        try {
            Product product = convertToEntity(request);
            Product updatedProduct = productService.updateProduct(productId, product);

            return ResponseEntity.ok(ApiResponse.<ProductDTO>builder()
                    .success(true)
                    .message("Product updated successfully")
                    .data(convertToDTO(updatedProduct))
                    .build());
        } catch (Exception e) {
            log.error("Error updating product {}: ", productId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<ProductDTO>builder()
                    .success(false)
                    .message("Error updating product: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Xóa sản phẩm (Admin only)
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long productId) {
        try {
            productService.deleteProduct(productId);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Product deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error deleting product {}: ", productId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Error deleting product: " + e.getMessage())
                    .build());
        }
    }

    // ===============================
    // CATEGORY ENDPOINTS
    // ===============================

    /**
     * Lấy tất cả category active
     */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getAllCategories() {
        try {
            List<Category> categories = productService.getAllActiveCategories();
            List<CategoryDTO> categoryDTOs = categories.stream()
                    .map(this::convertCategoryToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.<List<CategoryDTO>>builder()
                    .success(true)
                    .message("Categories retrieved successfully")
                    .data(categoryDTOs)
                    .build());
        } catch (Exception e) {
            log.error("Error retrieving categories: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<List<CategoryDTO>>builder()
                    .success(false)
                    .message("Error retrieving categories: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Lấy category có sản phẩm
     */
    @GetMapping("/categories/with-products")
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getCategoriesWithProducts() {
        try {
            List<Category> categories = productService.getCategoriesWithProducts();
            List<CategoryDTO> categoryDTOs = categories.stream()
                    .map(this::convertCategoryToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.<List<CategoryDTO>>builder()
                    .success(true)
                    .message("Categories with products retrieved successfully")
                    .data(categoryDTOs)
                    .build());
        } catch (Exception e) {
            log.error("Error retrieving categories with products: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<List<CategoryDTO>>builder()
                    .success(false)
                    .message("Error retrieving categories: " + e.getMessage())
                    .build());
        }
    }

    // ===============================
    // HELPER METHODS
    // ===============================

    private ProductDTO convertToDTO(Product product) {
        CategoryDTO categoryDTO = null;
        if (product.getCategory() != null) {
            categoryDTO = convertCategoryToDTO(product.getCategory());
        }

        // Convert options to DTOs
        List<ProductOptionDTO> optionDTOs = null;
        if (product.getOptions() != null && !product.getOptions().isEmpty()) {
            optionDTOs = product.getOptions().stream()
                    .map(this::convertOptionToDTO)
                    .collect(Collectors.toList());
        }

        return ProductDTO.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .galleryUrls(product.getGalleryUrlsList())
                .hasOptions(product.getHasOptions())
                .isAvailable(product.getIsAvailable())
                .isFeatured(product.getIsFeatured())
                .preparationTime(product.getPreparationTime())
                .category(categoryDTO)
                .options(optionDTOs) // Add options
                .salePrice(product.getSalePrice()) // Add sale fields
                .salePercentage(product.getSalePercentage())
                .isOnSale(product.getIsOnSale())
                .saleStartDate(product.getSaleStartDate() != null ? product.getSaleStartDate().toString() : null)
                .saleEndDate(product.getSaleEndDate() != null ? product.getSaleEndDate().toString() : null)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private CategoryDTO convertCategoryToDTO(Category category) {
        return CategoryDTO.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .categoryImageUrl(category.getCategoryImageUrl())
                .description(category.getDescription())
                .isActive(category.getIsActive())
                .sortOrder(category.getSortOrder())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private ProductOptionDTO convertOptionToDTO(com.example.food.model.ProductOption option) {
        return ProductOptionDTO.builder()
                .optionId(option.getOptionId())
                .productId(option.getProductId())
                .optionName(option.getOptionName())
                .optionType(option.getOptionType().toString())
                .price(option.getPrice())
                .isRequired(option.getIsRequired())
                .isActive(option.getIsActive())
                .maxSelections(option.getMaxSelections())
                .createdAt(option.getCreatedAt())
                .updatedAt(option.getUpdatedAt())
                .build();
    }

    private Product convertToEntity(CreateProductRequest request) {
        Product.ProductBuilder builder = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .hasOptions(request.getHasOptions())
                .isAvailable(request.getIsAvailable())
                .isFeatured(request.getIsFeatured())
                .preparationTime(request.getPreparationTime());

        if (request.getGalleryUrls() != null) {
            builder.galleryUrls(String.join(",", request.getGalleryUrls()));
        }

        if (request.getCategoryId() != null) {
            Category category = new Category();
            category.setCategoryId(request.getCategoryId());
            builder.category(category);
        }

        return builder.build();
    }

    private Product convertToEntity(UpdateProductRequest request) {
        Product.ProductBuilder builder = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .hasOptions(request.getHasOptions())
                .isAvailable(request.getIsAvailable())
                .isFeatured(request.getIsFeatured())
                .preparationTime(request.getPreparationTime());

        if (request.getGalleryUrls() != null) {
            builder.galleryUrls(String.join(",", request.getGalleryUrls()));
        }

        if (request.getCategoryId() != null) {
            Category category = new Category();
            category.setCategoryId(request.getCategoryId());
            builder.category(category);
        }

        return builder.build();
    }
}
