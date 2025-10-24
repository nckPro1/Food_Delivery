package com.example.food.controller.admin;

import com.example.food.dto.ApiResponse;
import com.example.food.dto.CategoryDTO;
import com.example.food.model.Category;
import com.example.food.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@Slf4j
public class AdminCategoryController {

    private final ProductService productService;

    // ===============================
    // WEB PAGES (Thymeleaf)
    // ===============================

    /**
     * Trang danh sách danh mục
     */
    @GetMapping
    public String categoriesPage(Model model) {
        try {
            List<Category> categories = productService.getAllActiveCategories();
            model.addAttribute("categories", categories);
            model.addAttribute("pageTitle", "Quản lý danh mục");
            return "admin/categories/list";
        } catch (Exception e) {
            log.error("Error loading categories page: ", e);
            model.addAttribute("error", "Lỗi tải danh sách danh mục: " + e.getMessage());
            return "admin/categories/list";
        }
    }

    /**
     * Trang tạo danh mục mới
     */
    @GetMapping("/create")
    public String createCategoryPage(Model model) {
        try {
            model.addAttribute("category", new Category());
            model.addAttribute("pageTitle", "Tạo danh mục mới");
            return "admin/categories/create";
        } catch (Exception e) {
            log.error("Error loading create category page: ", e);
            model.addAttribute("error", "Lỗi tải trang tạo danh mục: " + e.getMessage());
            return "admin/categories/create";
        }
    }

    /**
     * Trang chỉnh sửa danh mục
     */
    @GetMapping("/edit/{categoryId}")
    public String editCategoryPage(@PathVariable Long categoryId, Model model) {
        try {
            Category category = productService.getCategoryById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại"));

            model.addAttribute("category", category);
            model.addAttribute("pageTitle", "Chỉnh sửa danh mục: " + category.getCategoryName());

            return "admin/categories/edit";
        } catch (Exception e) {
            log.error("Error loading edit category page for ID {}: ", categoryId, e);
            model.addAttribute("error", "Lỗi tải trang chỉnh sửa: " + e.getMessage());
            return "redirect:/admin/categories";
        }
    }

    // ===============================
    // API ENDPOINTS (JSON)
    // ===============================

    /**
     * Tạo danh mục mới (Form submission)
     */
    @PostMapping("/create")
    public String createCategoryForm(@RequestParam String categoryName,
                                     @RequestParam(required = false) String description,
                                     @RequestParam(required = false) MultipartFile imageFile,
                                     @RequestParam(defaultValue = "0") int sortOrder,
                                     @RequestParam(defaultValue = "true") boolean isActive,
                                     Model model) {
        try {
            String imageUrl = "/images/default-avatar.jpg"; // Default image

            // Handle image upload if provided
            if (imageFile != null && !imageFile.isEmpty()) {
                imageUrl = saveImage(imageFile);
            }

            Category category = Category.builder()
                    .categoryName(categoryName)
                    .description(description)
                    .categoryImageUrl(imageUrl)
                    .sortOrder(sortOrder)
                    .isActive(isActive)
                    .build();

            Category createdCategory = productService.createCategory(category);

            log.info("Category created successfully: {}", createdCategory.getCategoryName());
            return "redirect:/admin/categories?success=created";
        } catch (Exception e) {
            log.error("Error creating category: ", e);
            model.addAttribute("error", "Lỗi tạo danh mục: " + e.getMessage());
            return "admin/categories/create";
        }
    }

    /**
     * Cập nhật danh mục (Form submission)
     */
    @PostMapping("/update/{categoryId}")
    public String updateCategoryForm(@PathVariable Long categoryId,
                                     @RequestParam String categoryName,
                                     @RequestParam(required = false) String description,
                                     @RequestParam(required = false) MultipartFile imageFile,
                                     @RequestParam(defaultValue = "0") int sortOrder,
                                     @RequestParam(defaultValue = "true") boolean isActive,
                                     @RequestParam(required = false) String removeImage,
                                     Model model) {
        try {
            // Get existing category
            Category existingCategory = productService.getCategoryById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại"));

            String imageUrl = existingCategory.getCategoryImageUrl(); // Keep existing image

            // Handle image removal
            if ("true".equals(removeImage)) {
                imageUrl = "/images/default-avatar.jpg";
            }
            // Handle new image upload if provided
            else if (imageFile != null && !imageFile.isEmpty()) {
                imageUrl = saveImage(imageFile);
            }

            Category category = Category.builder()
                    .categoryName(categoryName)
                    .description(description)
                    .categoryImageUrl(imageUrl)
                    .sortOrder(sortOrder)
                    .isActive(isActive)
                    .build();

            Category updatedCategory = productService.updateCategory(categoryId, category);

            log.info("Category updated successfully: {}", updatedCategory.getCategoryName());
            return "redirect:/admin/categories?success=updated";
        } catch (Exception e) {
            log.error("Error updating category: ", e);
            model.addAttribute("error", "Lỗi cập nhật danh mục: " + e.getMessage());

            // Reload the category data for the form
            try {
                Category category = productService.getCategoryById(categoryId)
                        .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại"));
                model.addAttribute("category", category);
                model.addAttribute("pageTitle", "Chỉnh sửa danh mục: " + category.getCategoryName());
            } catch (Exception ex) {
                log.error("Error loading category for error page: ", ex);
            }

            return "admin/categories/edit";
        }
    }

    /**
     * Tạo danh mục mới (API)
     */
    @PostMapping("/api/create")
    @ResponseBody
    public ResponseEntity<ApiResponse<CategoryDTO>> createCategory(@RequestBody CategoryDTO request) {
        try {
            Category category = Category.builder()
                    .categoryName(request.getCategoryName())
                    .categoryImageUrl(request.getCategoryImageUrl())
                    .description(request.getDescription())
                    .isActive(request.getIsActive())
                    .sortOrder(request.getSortOrder())
                    .build();

            Category createdCategory = productService.createCategory(category);

            return ResponseEntity.ok(ApiResponse.<CategoryDTO>builder()
                    .success(true)
                    .message("Tạo danh mục thành công")
                    .data(convertToDTO(createdCategory))
                    .build());
        } catch (Exception e) {
            log.error("Error creating category: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<CategoryDTO>builder()
                    .success(false)
                    .message("Lỗi tạo danh mục: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Cập nhật danh mục (API)
     */
    @PutMapping("/api/update/{categoryId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<CategoryDTO>> updateCategory(
            @PathVariable Long categoryId,
            @RequestBody CategoryDTO request) {
        try {
            Category category = Category.builder()
                    .categoryName(request.getCategoryName())
                    .categoryImageUrl(request.getCategoryImageUrl())
                    .description(request.getDescription())
                    .isActive(request.getIsActive())
                    .sortOrder(request.getSortOrder())
                    .build();

            Category updatedCategory = productService.updateCategory(categoryId, category);

            return ResponseEntity.ok(ApiResponse.<CategoryDTO>builder()
                    .success(true)
                    .message("Cập nhật danh mục thành công")
                    .data(convertToDTO(updatedCategory))
                    .build());
        } catch (Exception e) {
            log.error("Error updating category {}: ", categoryId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<CategoryDTO>builder()
                    .success(false)
                    .message("Lỗi cập nhật danh mục: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Xóa danh mục (API) - Soft delete
     */
    @DeleteMapping("/api/delete/{categoryId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long categoryId) {
        try {
            productService.deleteCategory(categoryId);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Xóa danh mục thành công")
                    .build());
        } catch (Exception e) {
            log.error("Error deleting category {}: ", categoryId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Lỗi xóa danh mục: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Xóa danh mục thật (API) - Hard delete
     */
    @DeleteMapping("/api/hard-delete/{categoryId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> hardDeleteCategory(@PathVariable Long categoryId) {
        try {
            productService.hardDeleteCategory(categoryId);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Xóa danh mục hoàn toàn thành công")
                    .build());
        } catch (Exception e) {
            log.error("Error hard deleting category {}: ", categoryId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Lỗi xóa danh mục: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Upload ảnh danh mục
     */
    @PostMapping("/api/upload-image/{categoryId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> uploadCategoryImage(
            @PathVariable Long categoryId,
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
            String filename = "category_" + categoryId + "_" + UUID.randomUUID().toString() + extension;

            // Create upload directory if not exists
            Path uploadDir = Paths.get("uploads/categories");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Save file
            Path filePath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Update category image URL
            String imageUrl = "/uploads/categories/" + filename;
            Category category = productService.getCategoryById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));

            category.setCategoryImageUrl(imageUrl);
            productService.updateCategory(categoryId, category);

            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true)
                    .message("Upload ảnh danh mục thành công")
                    .data(imageUrl)
                    .build());

        } catch (IOException e) {
            log.error("Error uploading category image for ID {}: ", categoryId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .success(false)
                    .message("Lỗi upload file: " + e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error uploading category image for ID {}: ", categoryId, e);
            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .success(false)
                    .message("Lỗi server: " + e.getMessage())
                    .build());
        }
    }

    // ===============================
    // HELPER METHODS
    // ===============================

    private String saveImage(MultipartFile imageFile) throws IOException {
        if (imageFile.isEmpty()) {
            return "/images/default-avatar.jpg";
        }

        // Create uploads directory if it doesn't exist
        String uploadDir = "uploads/categories";
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = imageFile.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String filename = UUID.randomUUID().toString() + fileExtension;

        // Save file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/categories/" + filename;
    }

    private CategoryDTO convertToDTO(Category category) {
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
}
