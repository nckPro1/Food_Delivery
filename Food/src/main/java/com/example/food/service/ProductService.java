package com.example.food.service;

import com.example.food.model.Product;
import com.example.food.model.Category;
import com.example.food.repository.ProductRepository;
import com.example.food.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // ===============================
    // PRODUCT OPERATIONS
    // ===============================

    /**
     * Lấy tất cả sản phẩm có sẵn với pagination
     */
    @Transactional(readOnly = true)
    public Page<Product> getAllAvailableProducts(Pageable pageable) {
        log.info("Fetching all available products with pagination: {}", pageable);
        return productRepository.findByIsAvailableTrue(pageable);
    }

    /**
     * Lấy sản phẩm theo ID
     */
    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long productId) {
        log.info("Fetching product by ID: {}", productId);
        return productRepository.findById(productId);
    }

    /**
     * Lấy sản phẩm theo category
     */
    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(Long categoryId) {
        log.info("Fetching products by category ID: {}", categoryId);
        return productRepository.findByCategoryCategoryIdAndIsAvailableTrue(categoryId);
    }

    /**
     * Lấy sản phẩm theo category với pagination
     */
    @Transactional(readOnly = true)
    public Page<Product> getProductsByCategory(Long categoryId, Pageable pageable) {
        log.info("Fetching products by category ID: {} with pagination: {}", categoryId, pageable);
        return productRepository.findByCategoryCategoryIdAndIsAvailableTrue(categoryId, pageable);
    }

    /**
     * Lấy sản phẩm featured
     */
    @Transactional(readOnly = true)
    public List<Product> getFeaturedProducts() {
        log.info("Fetching featured products");
        return productRepository.findByIsFeaturedTrueAndIsAvailableTrue();
    }

    /**
     * Tìm kiếm sản phẩm theo tên
     */
    @Transactional(readOnly = true)
    public Page<Product> searchProductsByName(String keyword, Pageable pageable) {
        log.info("Searching products by name: {} with pagination: {}", keyword, pageable);
        return productRepository.searchByName(keyword, pageable);
    }

    /**
     * Tìm kiếm sản phẩm theo tên và category
     */
    @Transactional(readOnly = true)
    public Page<Product> searchProductsByNameAndCategory(String keyword, Long categoryId, Pageable pageable) {
        log.info("Searching products by name: {} and category: {} with pagination: {}", keyword, categoryId, pageable);
        return productRepository.searchByNameAndCategory(keyword, categoryId, pageable);
    }

    /**
     * Tìm sản phẩm theo khoảng giá
     */
    @Transactional(readOnly = true)
    public Page<Product> getProductsByPriceRange(Double minPrice, Double maxPrice, Pageable pageable) {
        log.info("Fetching products by price range: {} - {} with pagination: {}", minPrice, maxPrice, pageable);
        return productRepository.findByPriceRange(minPrice, maxPrice, pageable);
    }

    /**
     * Lấy sản phẩm có nhiều ảnh
     */
    @Transactional(readOnly = true)
    public List<Product> getProductsWithGallery() {
        log.info("Fetching products with gallery images");
        return productRepository.findProductsWithGallery();
    }

    /**
     * Tạo sản phẩm mới
     */
    public Product createProduct(Product product) {
        log.info("Creating new product: {}", product.getName());

        // Validate category exists
        if (product.getCategory() != null && product.getCategory().getCategoryId() != null) {
            Optional<Category> category = categoryRepository.findById(product.getCategory().getCategoryId());
            if (category.isEmpty()) {
                throw new IllegalArgumentException("Category not found with ID: " + product.getCategory().getCategoryId());
            }
            product.setCategory(category.get());
        }

        return productRepository.save(product);
    }

    /**
     * Cập nhật sản phẩm
     */
    public Product updateProduct(Long productId, Product productDetails) {
        log.info("Updating product with ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        // Update fields
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setImageUrl(productDetails.getImageUrl());
        product.setGalleryUrls(productDetails.getGalleryUrls());
        product.setHasOptions(productDetails.getHasOptions());
        product.setIsAvailable(productDetails.getIsAvailable());
        product.setIsFeatured(productDetails.getIsFeatured());
        product.setPreparationTime(productDetails.getPreparationTime());

        // Update category if provided
        if (productDetails.getCategory() != null && productDetails.getCategory().getCategoryId() != null) {
            Optional<Category> category = categoryRepository.findById(productDetails.getCategory().getCategoryId());
            if (category.isEmpty()) {
                throw new IllegalArgumentException("Category not found with ID: " + productDetails.getCategory().getCategoryId());
            }
            product.setCategory(category.get());
        }

        return productRepository.save(product);
    }

    /**
     * Xóa sản phẩm (soft delete)
     */
    public void deleteProduct(Long productId) {
        log.info("Deleting product with ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        product.setIsAvailable(false);
        productRepository.save(product);
    }

    /**
     * Đếm số sản phẩm theo category
     */
    @Transactional(readOnly = true)
    public long countProductsByCategory(Long categoryId) {
        log.info("Counting products by category ID: {}", categoryId);
        return productRepository.countByCategoryCategoryIdAndIsAvailableTrue(categoryId);
    }

    // ===============================
    // CATEGORY OPERATIONS
    // ===============================

    /**
     * Lấy tất cả category active
     */
    @Transactional(readOnly = true)
    public List<Category> getAllActiveCategories() {
        log.info("Fetching all active categories");
        return categoryRepository.findByIsActiveTrueOrderBySortOrderAsc();
    }

    /**
     * Lấy category có sản phẩm
     */
    @Transactional(readOnly = true)
    public List<Category> getCategoriesWithProducts() {
        log.info("Fetching categories with products");
        return categoryRepository.findCategoriesWithProducts();
    }

    /**
     * Lấy category theo ID
     */
    @Transactional(readOnly = true)
    public Optional<Category> getCategoryById(Long categoryId) {
        log.info("Fetching category by ID: {}", categoryId);
        return categoryRepository.findById(categoryId);
    }

    /**
     * Tạo category mới
     */
    public Category createCategory(Category category) {
        log.info("Creating new category: {}", category.getCategoryName());

        // Check for duplicate category name (case insensitive)
        Optional<Category> existingCategory = categoryRepository.findByCategoryNameIgnoreCase(category.getCategoryName());
        if (existingCategory.isPresent()) {
            throw new IllegalArgumentException("Danh mục '" + category.getCategoryName() + "' đã tồn tại!");
        }

        return categoryRepository.save(category);
    }

    /**
     * Cập nhật category
     */
    public Category updateCategory(Long categoryId, Category categoryDetails) {
        log.info("Updating category with ID: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));

        category.setCategoryName(categoryDetails.getCategoryName());
        category.setCategoryImageUrl(categoryDetails.getCategoryImageUrl());
        category.setDescription(categoryDetails.getDescription());
        category.setIsActive(categoryDetails.getIsActive());
        category.setSortOrder(categoryDetails.getSortOrder());

        return categoryRepository.save(category);
    }

    /**
     * Xóa category (soft delete)
     */
    public void deleteCategory(Long categoryId) {
        log.info("Deleting category with ID: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));

        // Check if category has products
        long productCount = countProductsByCategory(categoryId);
        if (productCount > 0) {
            throw new IllegalArgumentException("Không thể xóa danh mục '" + category.getCategoryName() + "' vì còn " + productCount + " sản phẩm!");
        }

        category.setIsActive(false);
        categoryRepository.save(category);

        log.info("Category '{}' soft deleted successfully", category.getCategoryName());
    }

    /**
     * Xóa category thật (hard delete) - chỉ dùng khi cần thiết
     */
    public void hardDeleteCategory(Long categoryId) {
        log.info("Hard deleting category with ID: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));

        // Check if category has products
        long productCount = countProductsByCategory(categoryId);
        if (productCount > 0) {
            throw new IllegalArgumentException("Không thể xóa danh mục '" + category.getCategoryName() + "' vì còn " + productCount + " sản phẩm!");
        }

        categoryRepository.delete(category);
        log.info("Category '{}' hard deleted successfully", category.getCategoryName());
    }
}
