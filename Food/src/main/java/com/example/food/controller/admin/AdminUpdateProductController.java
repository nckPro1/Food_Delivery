package com.example.food.controller.admin;

import com.example.food.model.Product;
import com.example.food.model.Category;
import com.example.food.model.ProductOption;
import com.example.food.service.ProductService;
import com.example.food.repository.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@Slf4j
public class AdminUpdateProductController {

    private final ProductService productService;
    private final ProductOptionRepository productOptionRepository;

    /**
     * Xóa sản phẩm
     */
    @GetMapping("/delete/{productId}")
    public String deleteProduct(@PathVariable Long productId, Model model) {
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
     * Utility method để lưu hình ảnh
     */
    private String saveImage(MultipartFile file, String folder) throws IOException {
        if (file.isEmpty()) {
            return null;
        }

        // Tạo thư mục uploads nếu chưa có
        Path uploadDir = Paths.get("uploads", folder);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // Tạo tên file unique
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null ?
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        // Lưu file
        Path filePath = uploadDir.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Trả về đường dẫn relative
        return "/uploads/" + folder + "/" + uniqueFilename;
    }

    @PostMapping("/update/{productId}")  // Sửa lại từ /update/{id} -> /update/{productId} cho đồng bộ action form
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
            if (categoryId != null) {
                Category category = productService.getCategoryById(categoryId)
                        .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại"));
                existingProduct.setCategory(category);
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
                                .optionType(ProductOption.OptionType.valueOf(optionTypes != null && i < optionTypes.length ? optionTypes[i] : "SIZE"))
                                .price(optionPrices != null && i < optionPrices.length ? optionPrices[i] : BigDecimal.ZERO)
                                .isRequired(optionRequireds != null && i < optionRequireds.length ? optionRequireds[i] : false)
                                .maxSelections(optionMaxSelections != null && i < optionMaxSelections.length ? optionMaxSelections[i] : 1)
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
}
