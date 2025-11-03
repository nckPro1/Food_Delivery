package com.example.food.controller.admin;

import com.example.food.dto.*;
import com.example.food.model.Product;
import com.example.food.model.Category;
import com.example.food.model.ProductOption;
import com.example.food.service.ProductService;
import com.example.food.repository.ProductRepository;
import com.example.food.repository.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@Slf4j
public class AdminProductController {

    private final ProductService productService;
    private final ProductOptionRepository productOptionRepository;
    private final ProductRepository productRepository;

    // ===============================
    // WEB PAGES (Thymeleaf)
    // ===============================

    /**
     * Trang danh sách sản phẩm
     */
    @GetMapping("/simple")
    public String simpleProducts(Model model) {
        model.addAttribute("pageTitle", "Sản phẩm - Đơn giản");
        return "admin/products/simple";
    }

    @GetMapping
    public String productsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "false") boolean inactive,
            Model model) {

        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ?
                    Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<Product> products;
            if (inactive) {
                if (search != null && !search.isEmpty()) {
                    if (categoryId != null) {
                        products = productService.searchInactiveProductsByNameAndCategory(search, categoryId, pageable);
                    } else {
                        products = productService.searchInactiveProductsByName(search, pageable);
                    }
                } else if (categoryId != null) {
                    products = productService.getInactiveProductsByCategory(categoryId, pageable);
                } else {
                    products = productService.getAllInactiveProducts(pageable);
                }
            } else {
                if (search != null && !search.isEmpty()) {
                    if (categoryId != null) {
                        products = productService.searchProductsByNameAndCategory(search, categoryId, pageable);
                    } else {
                        products = productService.searchProductsByName(search, pageable);
                    }
                } else if (categoryId != null) {
                    products = productService.getProductsByCategory(categoryId, pageable);
                } else {
                    products = productService.getAllAvailableProducts(pageable);
                }
            }

            List<Category> categories = productService.getAllActiveCategories();

            model.addAttribute("products", products);
            model.addAttribute("categories", categories);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", products.getTotalPages());
            model.addAttribute("totalElements", products.getTotalElements());
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);
            model.addAttribute("search", search);
            model.addAttribute("categoryId", categoryId);
            model.addAttribute("pageTitle", inactive ? "Sản phẩm - Ngưng hiển thị" : "Sản phẩm - Danh sách");
            model.addAttribute("inactive", inactive);

            return "admin/products/list";
        } catch (Exception e) {
            log.error("Error loading products page: ", e);
            model.addAttribute("error", "Lỗi tải danh sách sản phẩm: " + e.getMessage());
            model.addAttribute("pageTitle", "Sản phẩm - Danh sách");
            return "admin/products/list";
        }
    }

    /**
     * Trang tạo sản phẩm mới
     */
    @GetMapping("/create")
    public String createProductPage(Model model) {
        try {
            List<Category> categories = productService.getAllActiveCategories();
            model.addAttribute("categories", categories);
            model.addAttribute("product", new Product());
            model.addAttribute("pageTitle", "Sản phẩm - Tạo mới");
            return "admin/products/create";
        } catch (Exception e) {
            log.error("Error loading create product page: ", e);
            model.addAttribute("error", "Lỗi tải trang tạo sản phẩm: " + e.getMessage());
            model.addAttribute("pageTitle", "Sản phẩm - Tạo mới");
            return "admin/products/create";
        }
    }

    /**
     * Trang chỉnh sửa sản phẩm
     */
    @GetMapping("/edit/{productId}")
    public String editProductPage(@PathVariable Long productId, Model model) {
        try {
            Product product = productService.getProductById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));

            List<Category> categories = productService.getAllActiveCategories();

            model.addAttribute("product", product);
            model.addAttribute("categories", categories);
            model.addAttribute("pageTitle", "Sản phẩm - Chỉnh sửa");

            return "admin/products/edit";
        } catch (Exception e) {
            log.error("Error loading edit product page for ID {}: ", productId, e);
            model.addAttribute("error", "Lỗi tải trang chỉnh sửa: " + e.getMessage());
            return "redirect:/admin/products";
        }
    }

    /**
     * Trang chi tiết sản phẩm
     */
    @GetMapping("/view/{productId}")
    public String viewProductPage(@PathVariable Long productId, Model model) {
        try {
            Product product = productService.getProductById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));

            model.addAttribute("product", product);
            model.addAttribute("pageTitle", "Sản phẩm - Chi tiết");

            return "admin/products/view";
        } catch (Exception e) {
            log.error("Error loading view product page for ID {}: ", productId, e);
            model.addAttribute("error", "Lỗi tải trang chi tiết: " + e.getMessage());
            return "redirect:/admin/products";
        }
    }

    // ===============================
    // API ENDPOINTS (JSON)
    // ===============================

    /**
     * Tạo sản phẩm mới (Form submission)
     */
    @PostMapping("/create")
    public String createProductForm(@RequestParam String name,
                                    @RequestParam(required = false) String description,
                                    @RequestParam double price,
                                    @RequestParam(required = false) Long categoryId,
                                    @RequestParam(required = false) MultipartFile imageFile,
                                    @RequestParam(required = false) MultipartFile[] galleryFiles,
                                    @RequestParam(defaultValue = "true") boolean isAvailable,
                                    @RequestParam(defaultValue = "false") boolean isFeatured,
                                    @RequestParam(defaultValue = "false") boolean hasOptions,
                                    @RequestParam(required = false) String[] optionNames,
                                    @RequestParam(required = false) String[] optionTypes,
                                    @RequestParam(required = false) Double[] optionPrices,
                                    @RequestParam(required = false) Boolean[] optionRequireds,
                                    Model model) {
        try {
            String imageUrl = "/images/default-avatar.jpg"; // Default image

            // Handle main image upload if provided
            if (imageFile != null && !imageFile.isEmpty()) {
                imageUrl = saveImage(imageFile, "products");
            }

            // Handle gallery images
            String galleryUrls = "[]"; // Default empty JSON array
            if (galleryFiles != null && galleryFiles.length > 0) {
                List<String> galleryList = new ArrayList<>();
                for (MultipartFile file : galleryFiles) {
                    if (!file.isEmpty()) {
                        String galleryUrl = saveImage(file, "products/gallery");
                        galleryList.add(galleryUrl);
                    }
                }
                if (!galleryList.isEmpty()) {
                    galleryUrls = "[\"" + String.join("\",\"", galleryList) + "\"]";
                }
            }

            // Get category
            Category category = null;
            if (categoryId != null) {
                category = productService.getCategoryById(categoryId).orElse(null);
            }

            Product product = Product.builder()
                    .name(name)
                    .description(description)
                    .price(BigDecimal.valueOf(price))
                    .imageUrl(imageUrl)
                    .galleryUrls(galleryUrls)
                    .category(category)
                    .isAvailable(isAvailable)
                    .isFeatured(isFeatured)
                    .hasOptions(hasOptions)
                    .build();

            Product createdProduct = productService.createProduct(product);

            // Save product options if hasOptions is true and options are provided
            if (hasOptions && optionNames != null && optionNames.length > 0) {
                for (int i = 0; i < optionNames.length; i++) {
                    if (optionNames[i] != null && !optionNames[i].trim().isEmpty()) {
                        ProductOption option = ProductOption.builder()
                                .productId(createdProduct.getProductId())
                                .optionName(optionNames[i].trim())
                                .optionType(ProductOption.OptionType.valueOf(
                                        optionTypes != null && i < optionTypes.length ?
                                                optionTypes[i].toUpperCase() : "EXTRA"))
                                .price(BigDecimal.valueOf(
                                        optionPrices != null && i < optionPrices.length && optionPrices[i] != null ?
                                                optionPrices[i] : 0.0))
                                .isRequired(optionRequireds != null && i < optionRequireds.length &&
                                        optionRequireds[i] != null ? optionRequireds[i] : false)
                                .maxSelections(1)
                                .build();

                        productOptionRepository.save(option);
                    }
                }
            }

            log.info("Product created successfully: {}", createdProduct.getName());
            return "redirect:/admin/products?success=created";
        } catch (Exception e) {
            log.error("Error creating product: ", e);
            model.addAttribute("error", "Lỗi tạo sản phẩm: " + e.getMessage());
            model.addAttribute("pageTitle", "Sản phẩm - Tạo mới");
            try {
                List<Category> categories = productService.getAllActiveCategories();
                model.addAttribute("categories", categories);
            } catch (Exception ex) {
                log.error("Error loading categories: ", ex);
            }
            return "admin/products/create";
        }
    }

    /**
     * Tạo sản phẩm mới (API)
     */
    @PostMapping("/api/create")
    @ResponseBody
    public ResponseEntity<ApiResponse<ProductDTO>> createProduct(@RequestBody CreateProductRequest request) {
        try {
            Product product = convertToEntity(request);
            Product createdProduct = productService.createProduct(product);

            return ResponseEntity.ok(ApiResponse.<ProductDTO>builder()
                    .success(true)
                    .message("Tạo sản phẩm thành công")
                    .data(convertToDTO(createdProduct))
                    .build());
        } catch (Exception e) {
            log.error("Error creating product: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<ProductDTO>builder()
                    .success(false)
                    .message("Lỗi tạo sản phẩm: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Cập nhật sản phẩm (API)
     */
    @PutMapping("/api/update/{productId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<ProductDTO>> updateProduct(
            @PathVariable Long productId,
            @RequestBody UpdateProductRequest request) {
        try {
            Product product = convertToEntity(request);
            Product updatedProduct = productService.updateProduct(productId, product);

            return ResponseEntity.ok(ApiResponse.<ProductDTO>builder()
                    .success(true)
                    .message("Cập nhật sản phẩm thành công")
                    .data(convertToDTO(updatedProduct))
                    .build());
        } catch (Exception e) {
            log.error("Error updating product {}: ", productId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<ProductDTO>builder()
                    .success(false)
                    .message("Lỗi cập nhật sản phẩm: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Cập nhật cờ Featured cho sản phẩm (API)
     */
    @PutMapping("/api/{productId}/featured")
    @ResponseBody
    public ResponseEntity<ApiResponse<ProductDTO>> updateFeatured(
            @PathVariable Long productId,
            @RequestParam("value") boolean isFeatured) {
        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));
            product.setIsFeatured(isFeatured);
            Product saved = productRepository.save(product);

            return ResponseEntity.ok(ApiResponse.<ProductDTO>builder()
                    .success(true)
                    .message("Cập nhật món tiêu biểu thành công")
                    .data(convertToDTO(saved))
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.<ProductDTO>builder()
                    .success(false)
                    .message("Lỗi cập nhật món tiêu biểu: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Cập nhật trạng thái hiển thị (isAvailable) cho sản phẩm (API)
     */
    @PutMapping("/api/{productId}/availability")
    @ResponseBody
    public ResponseEntity<ApiResponse<ProductDTO>> updateAvailability(
            @PathVariable Long productId,
            @RequestParam("value") boolean isAvailable) {
        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));
            product.setIsAvailable(isAvailable);
            Product saved = productRepository.save(product);

            return ResponseEntity.ok(ApiResponse.<ProductDTO>builder()
                    .success(true)
                    .message("Cập nhật trạng thái hiển thị thành công")
                    .data(convertToDTO(saved))
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.<ProductDTO>builder()
                    .success(false)
                    .message("Lỗi cập nhật trạng thái hiển thị: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Cập nhật sản phẩm (Form submission)
     */
    @PostMapping("/update/{productId}")
    public String updateProduct(
            @PathVariable("productId") Long productId,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam double price,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) MultipartFile mainImage,
            @RequestParam(required = false) MultipartFile[] galleryImages,
            @RequestParam(defaultValue = "true") boolean isAvailable,
            @RequestParam(defaultValue = "false") boolean isFeatured,
            @RequestParam(defaultValue = "false") boolean hasOptions,
            @RequestParam(required = false) String[] optionNames,
            @RequestParam(required = false) String[] optionTypes,
            @RequestParam(required = false) BigDecimal[] optionPrices,
            @RequestParam(required = false) Boolean[] optionRequireds,
            @RequestParam(required = false) Integer[] optionMaxSelections,
            @RequestParam(defaultValue = "15") int preparationTime,
            Model model) {
        try {
            // Get existing product
            Product existingProduct = productService.getProductById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));

            // Update basic info
            existingProduct.setName(name);
            existingProduct.setDescription(description);
            existingProduct.setPrice(BigDecimal.valueOf(price));
            existingProduct.setIsAvailable(isAvailable);
            existingProduct.setIsFeatured(isFeatured);
            existingProduct.setHasOptions(hasOptions);
            existingProduct.setPreparationTime(preparationTime);

            // Update category
            if (categoryId != null && categoryId > 0) {
                Category category = productService.getCategoryById(categoryId)
                        .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại"));
                existingProduct.setCategory(category);
            } else {
                // If categoryId is null or 0, remove category
                existingProduct.setCategory(null);
            }

            // Handle main image
            if (mainImage != null && !mainImage.isEmpty()) {
                String mainImageUrl = saveImage(mainImage, "products");
                existingProduct.setImageUrl(mainImageUrl);
            }

            // Handle gallery images
            if (galleryImages != null && galleryImages.length > 0) {
                List<String> galleryUrls = new ArrayList<>();
                for (MultipartFile file : galleryImages) {
                    if (!file.isEmpty()) {
                        String galleryUrl = saveImage(file, "products/gallery");
                        galleryUrls.add(galleryUrl);
                    }
                }
                if (!galleryUrls.isEmpty()) {
                    existingProduct.setGalleryUrls(String.join(",", galleryUrls));
                }
            }

            // Save product
            Product updatedProduct = productService.updateProduct(productId, existingProduct);

            // Handle product options
            if (hasOptions && optionNames != null && optionNames.length > 0) {
                // Delete existing options
                productOptionRepository.deleteByProductId(productId);

                // Create new options
                for (int i = 0; i < optionNames.length; i++) {
                    if (optionNames[i] != null && !optionNames[i].trim().isEmpty()) {
                        ProductOption option = ProductOption.builder()
                                .productId(updatedProduct.getProductId())
                                .optionName(optionNames[i].trim())
                                .optionType(ProductOption.OptionType.valueOf(
                                        optionTypes != null && i < optionTypes.length && optionTypes[i] != null ?
                                                optionTypes[i].toUpperCase() : "SIZE"))
                                .price(optionPrices != null && i < optionPrices.length && optionPrices[i] != null ? optionPrices[i] : BigDecimal.ZERO)
                                .isRequired(optionRequireds != null && i < optionRequireds.length && optionRequireds[i] != null ? optionRequireds[i] : false)
                                .maxSelections(optionMaxSelections != null && i < optionMaxSelections.length && optionMaxSelections[i] != null ? optionMaxSelections[i] : 1)
                                .build();

                        productOptionRepository.save(option);
                    }
                }
            } else if (!hasOptions) {
                // Delete all options if hasOptions is false
                productOptionRepository.deleteByProductId(productId);
            }

            model.addAttribute("success", "Cập nhật sản phẩm thành công!");
            return "redirect:/admin/products/view/" + updatedProduct.getProductId();

        } catch (Exception e) {
            log.error("Error updating product: ", e);
            model.addAttribute("error", "Lỗi cập nhật sản phẩm: " + e.getMessage());
            return "redirect:/admin/products/edit/" + productId;
        }
    }

    /**
     * Xóa sản phẩm (Form submission)
     */
    @GetMapping("/delete/{productId}")
    public String deleteProductForm(@PathVariable Long productId, Model model) {
        try {
            productService.deleteProduct(productId);
            model.addAttribute("success", "Xóa sản phẩm thành công!");
        } catch (Exception e) {
            log.error("Error deleting product: ", e);
            model.addAttribute("error", "Lỗi xóa sản phẩm: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    /**
     * Xóa sản phẩm (API)
     */
    @DeleteMapping("/api/delete/{productId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long productId) {
        try {
            productService.deleteProduct(productId);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Xóa sản phẩm thành công")
                    .build());
        } catch (Exception e) {
            log.error("Error deleting product {}: ", productId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Lỗi xóa sản phẩm: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Upload ảnh chính cho sản phẩm
     */
    @PostMapping("/api/upload-main-image/{productId}")
    @ResponseBody
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

    // ===============================
    // TEST METHODS
    // ===============================

    @PostMapping("/create-test-product-with-options")
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> createTestProductWithOptions() {
        try {
            // Create a test product
            Product testProduct = Product.builder()
                    .name("Pizza Test với Options")
                    .description("Pizza test để kiểm tra options")
                    .price(new java.math.BigDecimal("150000"))
                    .imageUrl("/images/default-pizza.jpg")
                    .galleryUrls("/images/default-pizza-1.jpg,/images/default-pizza-2.jpg")
                    .hasOptions(true)
                    .isAvailable(true)
                    .isFeatured(false)
                    .preparationTime(20)
                    .build();

            // Get first category
            List<Category> categories = productService.getAllActiveCategories();
            if (!categories.isEmpty()) {
                testProduct.setCategory(categories.get(0));
            }

            Product savedProduct = productService.createProduct(testProduct);

            // Create options for the product
            List<ProductOption> options = java.util.Arrays.asList(
                    ProductOption.builder()
                            .productId(savedProduct.getProductId())
                            .optionName("Small (10\")")
                            .optionType(ProductOption.OptionType.SIZE)
                            .price(java.math.BigDecimal.ZERO)
                            .isRequired(true)
                            .maxSelections(1)
                            .build(),
                    ProductOption.builder()
                            .productId(savedProduct.getProductId())
                            .optionName("Medium (12\")")
                            .optionType(ProductOption.OptionType.SIZE)
                            .price(new java.math.BigDecimal("20000"))
                            .isRequired(true)
                            .maxSelections(1)
                            .build(),
                    ProductOption.builder()
                            .productId(savedProduct.getProductId())
                            .optionName("Large (14\")")
                            .optionType(ProductOption.OptionType.SIZE)
                            .price(new java.math.BigDecimal("40000"))
                            .isRequired(true)
                            .maxSelections(1)
                            .build(),
                    ProductOption.builder()
                            .productId(savedProduct.getProductId())
                            .optionName("Extra Cheese")
                            .optionType(ProductOption.OptionType.TOPPING)
                            .price(new java.math.BigDecimal("15000"))
                            .isRequired(false)
                            .maxSelections(3)
                            .build(),
                    ProductOption.builder()
                            .productId(savedProduct.getProductId())
                            .optionName("Extra Meat")
                            .optionType(ProductOption.OptionType.TOPPING)
                            .price(new java.math.BigDecimal("25000"))
                            .isRequired(false)
                            .maxSelections(3)
                            .build()
            );

            // Save options
            for (ProductOption option : options) {
                productOptionRepository.save(option);
            }

            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true)
                    .message("Test product with options created successfully. Product ID: " + savedProduct.getProductId())
                    .data("Product ID: " + savedProduct.getProductId())
                    .build());

        } catch (Exception e) {
            log.error("Error creating test product with options: ", e);
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                    .success(false)
                    .message("Error creating test product: " + e.getMessage())
                    .build());
        }
    }

    // ===============================
    // HELPER METHODS
    // ===============================

    private String saveImage(MultipartFile imageFile, String subfolder) throws IOException {
        if (imageFile.isEmpty()) {
            return "/images/default-avatar.jpg";
        }

        // Create uploads directory if it doesn't exist
        String uploadDir = "uploads/" + subfolder;
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = imageFile.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = "image.jpg";
        }
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String filename = UUID.randomUUID().toString() + fileExtension;

        // Save file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/" + subfolder + "/" + filename;
    }

    private ProductDTO convertToDTO(Product product) {
        CategoryDTO categoryDTO = null;
        if (product.getCategory() != null) {
            categoryDTO = CategoryDTO.builder()
                    .categoryId(product.getCategory().getCategoryId())
                    .categoryName(product.getCategory().getCategoryName())
                    .categoryImageUrl(product.getCategory().getCategoryImageUrl())
                    .description(product.getCategory().getDescription())
                    .isActive(product.getCategory().getIsActive())
                    .sortOrder(product.getCategory().getSortOrder())
                    .createdAt(product.getCategory().getCreatedAt())
                    .updatedAt(product.getCategory().getUpdatedAt())
                    .build();
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
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
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

    // ===============================
    // PRODUCT OPTIONS ENDPOINTS
    // ===============================

    @GetMapping("/{productId}/options")
    @ResponseBody
    public ResponseEntity<com.example.food.dto.ApiResponse<List<ProductOptionDTO>>> getProductOptions(@PathVariable Long productId) {
        try {
            List<ProductOption> options = productOptionRepository.findByProductIdOrderByOptionTypeAscOptionNameAsc(productId);
            List<ProductOptionDTO> optionDTOs = options.stream()
                    .map(this::convertToDTO)
                    .toList();

            com.example.food.dto.ApiResponse<List<ProductOptionDTO>> response = com.example.food.dto.ApiResponse.<List<ProductOptionDTO>>builder()
                    .success(true)
                    .message("Options retrieved successfully")
                    .data(optionDTOs)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching product options for product ID: {}", productId, e);
            com.example.food.dto.ApiResponse<List<ProductOptionDTO>> errorResponse = com.example.food.dto.ApiResponse.<List<ProductOptionDTO>>builder()
                    .success(false)
                    .message("Error fetching options: " + e.getMessage())
                    .data(null)
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/{productId}/options")
    @ResponseBody
    public ResponseEntity<ProductOptionDTO> createProductOption(
            @PathVariable Long productId,
            @RequestBody CreateProductOptionRequest request) {
        try {
            ProductOption option = ProductOption.builder()
                    .productId(productId)
                    .optionName(request.getOptionName())
                    .optionType(ProductOption.OptionType.valueOf(request.getOptionType().toUpperCase()))
                    .price(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO)
                    .isRequired(request.getIsRequired() != null ? request.getIsRequired() : false)
                    .maxSelections(request.getMaxSelections() != null ? request.getMaxSelections() : 1)
                    .build();

            ProductOption savedOption = productOptionRepository.save(option);
            ProductOptionDTO optionDTO = convertToDTO(savedOption);
            return ResponseEntity.ok(optionDTO);
        } catch (Exception e) {
            log.error("Error creating product option for product ID: {}", productId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/options/{optionId}")
    @ResponseBody
    public ResponseEntity<Void> deleteProductOption(@PathVariable Long optionId) {
        try {
            productOptionRepository.deleteById(optionId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting product option ID: {}", optionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private ProductOptionDTO convertToDTO(ProductOption option) {
        return ProductOptionDTO.builder()
                .optionId(option.getOptionId())
                .productId(option.getProductId())
                .optionName(option.getOptionName())
                .optionType(option.getOptionType().getValue())
                .price(option.getPrice())
                .isRequired(option.getIsRequired())
                .maxSelections(option.getMaxSelections())
                .createdAt(option.getCreatedAt())
                .updatedAt(option.getUpdatedAt())
                .build();
    }
}
