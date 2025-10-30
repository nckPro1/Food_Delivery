package com.example.food.service;

import com.example.food.dto.CreateSaleRequest;
import com.example.food.dto.SaleDTO;
import com.example.food.model.Product;
import com.example.food.model.Sale;
import com.example.food.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductService productService;

    public List<SaleDTO> getAllSales() {
        return saleRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<SaleDTO> getActiveSales() {
        return saleRepository.findAllActiveSales(LocalDateTime.now()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<SaleDTO> getExpiredSales() {
        LocalDateTime now = LocalDateTime.now();
        return saleRepository.findAll().stream()
                .filter(sale -> sale.getEndDate().isBefore(now) && sale.getIsActive())
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public SaleDTO getSaleById(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Sale not found"));
        return convertToDTO(sale);
    }

    @Transactional
    public SaleDTO createSale(CreateSaleRequest request) {
        validateSaleRequest(request);

        LocalDateTime now = LocalDateTime.now();

        // Kiểm tra đã có sale active trên sản phẩm chưa
        if (request.getProductId() != null) {
            checkForExistingActiveSale(request.getProductId(), now);
        }

        Sale sale = buildSaleFromRequest(request);
        Sale savedSale = saleRepository.save(sale);

        // Update product sale fields nếu sale đã bắt đầu
        if (isSaleCurrentlyActive(savedSale, now)) {
            updateProductSaleFields(savedSale);
        }

        log.info("Sale '{}' đã được tạo với ID: {}", savedSale.getSaleName(), savedSale.getSaleId());
        return convertToDTO(savedSale);
    }

    @Transactional
    public SaleDTO updateSale(Long saleId, CreateSaleRequest request) {
        validateSaleRequest(request);

        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Sale not found with ID: " + saleId));

        updateSaleFromRequest(sale, request);
        Sale savedSale = saleRepository.save(sale);

        // Update product sale fields based on current status
        updateProductFieldsAfterSaleUpdate(savedSale);

        log.info("Sale '{}' (ID: {}) đã được cập nhật", savedSale.getSaleName(), savedSale.getSaleId());
        return convertToDTO(savedSale);
    }

    @Transactional
    public void deleteSale(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Sale not found with ID: " + saleId));

        // Clear product sale fields trước khi xóa
        if (sale.getProductId() != null) {
            clearProductSaleFields(sale.getProductId());
        }

        saleRepository.delete(sale);
        log.info("Sale '{}' với ID {} đã được xóa", sale.getSaleName(), saleId);
    }

    @Transactional
    public void deleteSales(List<Long> saleIds) {
        if (saleIds == null || saleIds.isEmpty()) {
            return;
        }

        List<Sale> sales = saleRepository.findAllById(saleIds);

        // Clear product sale fields cho tất cả sales
        sales.forEach(sale -> {
            if (sale.getProductId() != null) {
                clearProductSaleFields(sale.getProductId());
            }
        });

        saleRepository.deleteAll(sales);
        log.info("Đã xóa {} sale(s)", sales.size());
    }

    @Transactional
    public SaleDTO toggleSaleStatus(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Sale not found with ID: " + saleId));

        boolean newStatus = !sale.getIsActive();
        sale.setIsActive(newStatus);
        Sale savedSale = saleRepository.save(sale);

        // Update product fields based on new status
        updateProductFieldsAfterStatusToggle(savedSale);

        log.info("Sale '{}' (ID: {}) đã được {}", savedSale.getSaleName(), savedSale.getSaleId(),
                newStatus ? "kích hoạt" : "tạm dừng");
        return convertToDTO(savedSale);
    }

    /**
     * Tự động deactivate các sale đã quá hạn - chạy mỗi giờ
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void deactivateExpiredSales() {
        LocalDateTime now = LocalDateTime.now();

        List<Sale> expiredSales = saleRepository.findAll().stream()
                .filter(sale -> sale.getIsActive() && sale.getEndDate().isBefore(now))
                .collect(Collectors.toList());

        if (!expiredSales.isEmpty()) {
            expiredSales.forEach(sale -> {
                sale.setIsActive(false);
                clearProductSaleFields(sale.getProductId());
            });

            saleRepository.saveAll(expiredSales);
            log.info("Đã tự động deactivate {} sale(s) đã quá hạn", expiredSales.size());
        }
    }

    /**
     * Tự động activate các sale đã đến thời gian bắt đầu - chạy mỗi giờ
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void activateScheduledSales() {
        LocalDateTime now = LocalDateTime.now();

        List<Sale> scheduledSales = saleRepository.findAll().stream()
                .filter(sale -> sale.getIsActive() &&
                        sale.getStartDate().isBefore(now) &&
                        sale.getEndDate().isAfter(now))
                .collect(Collectors.toList());

        scheduledSales.forEach(this::updateProductSaleFields);

        if (!scheduledSales.isEmpty()) {
            log.info("Đã tự động activate {} sale(s) theo lịch trình", scheduledSales.size());
        }
    }

    /**
     * Cleanup các sale đã quá hạn lâu - chạy hàng ngày lúc 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldExpiredSales() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);

        List<Sale> oldExpiredSales = saleRepository.findAll().stream()
                .filter(sale -> !sale.getIsActive() && sale.getEndDate().isBefore(cutoffDate))
                .collect(Collectors.toList());

        if (!oldExpiredSales.isEmpty()) {
            saleRepository.deleteAll(oldExpiredSales);
            log.info("Đã cleanup {} sale(s) cũ đã quá hạn trên 30 ngày", oldExpiredSales.size());
        }
    }

    /**
     * Lấy thống kê về các sale
     */
    public SaleStatistics getSaleStatistics() {
        LocalDateTime now = LocalDateTime.now();
        List<Sale> allSales = saleRepository.findAll();

        long activeSales = allSales.stream()
                .filter(sale -> isSaleCurrentlyActive(sale, now))
                .count();

        long expiredSales = allSales.stream()
                .filter(sale -> sale.getEndDate().isBefore(now))
                .count();

        long scheduledSales = allSales.stream()
                .filter(sale -> sale.getIsActive() && sale.getStartDate().isAfter(now))
                .count();

        long inactiveSales = allSales.stream()
                .filter(sale -> !sale.getIsActive())
                .count();

        return SaleStatistics.builder()
                .totalSales(allSales.size())
                .activeSales((int) activeSales)
                .expiredSales((int) expiredSales)
                .scheduledSales((int) scheduledSales)
                .inactiveSales((int) inactiveSales)
                .build();
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private void validateSaleRequest(CreateSaleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Sale request cannot be null");
        }

        if (request.getEndDate() != null && request.getStartDate() != null &&
                request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }

        if (request.getDiscountValue() != null && request.getDiscountValue().doubleValue() <= 0) {
            throw new IllegalArgumentException("Giá trị giảm giá phải lớn hơn 0");
        }

        if ("PERCENTAGE".equals(request.getDiscountType()) &&
                request.getDiscountValue() != null &&
                request.getDiscountValue().doubleValue() > 100) {
            throw new IllegalArgumentException("Phần trăm giảm giá không thể vượt quá 100%");
        }
    }

    private void checkForExistingActiveSale(Long productId, LocalDateTime now) {
        boolean hasActiveSale = saleRepository
                .findByProductIdAndIsActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        productId, now, now
                ).isPresent();

        if (hasActiveSale) {
            throw new RuntimeException("Sản phẩm này đang trong thời gian khuyến mãi. Không thể tạo thêm sale mới.");
        }
    }

    private Sale buildSaleFromRequest(CreateSaleRequest request) {
        return Sale.builder()
                .productId(request.getProductId())
                .saleName(request.getSaleName())
                .saleDescription(request.getSaleDescription())
                .discountType(Sale.DiscountType.valueOf(request.getDiscountType()))
                .discountValue(request.getDiscountValue())
                .salePrice(request.getSalePrice())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(true)
                .build();
    }

    private void updateSaleFromRequest(Sale sale, CreateSaleRequest request) {
        sale.setSaleName(request.getSaleName());
        sale.setSaleDescription(request.getSaleDescription());
        sale.setDiscountType(Sale.DiscountType.valueOf(request.getDiscountType()));
        sale.setDiscountValue(request.getDiscountValue());
        sale.setSalePrice(request.getSalePrice());
        sale.setStartDate(request.getStartDate());
        sale.setEndDate(request.getEndDate());
    }

    private void updateProductFieldsAfterSaleUpdate(Sale sale) {
        if (!sale.getIsActive()) {
            clearProductSaleFields(sale.getProductId());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (isSaleCurrentlyActive(sale, now)) {
            updateProductSaleFields(sale);
        } else {
            clearProductSaleFields(sale.getProductId());
        }
    }

    private void updateProductFieldsAfterStatusToggle(Sale sale) {
        LocalDateTime now = LocalDateTime.now();

        if (sale.getIsActive() && isSaleCurrentlyActive(sale, now)) {
            updateProductSaleFields(sale);
        } else {
            clearProductSaleFields(sale.getProductId());
        }
    }

    private boolean isSaleCurrentlyActive(Sale sale, LocalDateTime now) {
        return sale.getIsActive() &&
                sale.getStartDate().isBefore(now) &&
                sale.getEndDate().isAfter(now);
    }

    private void updateProductSaleFields(Sale sale) {
        if (sale.getProductId() == null) {
            log.warn("Cannot update product sale fields: productId is null for sale ID {}", sale.getSaleId());
            return;
        }

        try {
            Optional<Product> productOpt = productService.getProductById(sale.getProductId());
            if (productOpt.isEmpty()) {
                log.warn("Product not found with ID {} for sale ID {}", sale.getProductId(), sale.getSaleId());
                return;
            }

            Product product = productOpt.get();
            product.setIsOnSale(true);
            product.setSalePrice(sale.getSalePrice());
            product.setSalePercentage(sale.getDiscountType() == Sale.DiscountType.PERCENTAGE ?
                    sale.getDiscountValue().intValue() : null);
            product.setSaleStartDate(sale.getStartDate());
            product.setSaleEndDate(sale.getEndDate());

            productService.updateProduct(product.getProductId(), product);
            log.debug("Updated product sale fields for product ID {}", product.getProductId());
        } catch (Exception e) {
            log.error("Error updating product sale fields for product ID {}: {}", sale.getProductId(), e.getMessage());
        }
    }

    private void clearProductSaleFields(Long productId) {
        if (productId == null) {
            return;
        }

        try {
            Optional<Product> productOpt = productService.getProductById(productId);
            if (productOpt.isEmpty()) {
                log.debug("Product not found with ID {} - may have been deleted", productId);
                return;
            }

            Product product = productOpt.get();
            product.setIsOnSale(false);
            product.setSalePrice(null);
            product.setSalePercentage(null);
            product.setSaleStartDate(null);
            product.setSaleEndDate(null);

            productService.updateProduct(product.getProductId(), product);
            log.debug("Cleared product sale fields for product ID {}", productId);
        } catch (Exception e) {
            log.warn("Cannot clear sale fields for product ID {}: {}", productId, e.getMessage());
        }
    }

    private SaleDTO convertToDTO(Sale sale) {
        SaleDTO.SaleDTOBuilder builder = SaleDTO.builder()
                .saleId(sale.getSaleId())
                .productId(sale.getProductId())
                .saleName(sale.getSaleName())
                .saleDescription(sale.getSaleDescription())
                .discountType(sale.getDiscountType().name())
                .discountValue(sale.getDiscountValue())
                .salePrice(sale.getSalePrice())
                .startDate(sale.getStartDate())
                .endDate(sale.getEndDate())
                .isActive(sale.getIsActive())
                .createdAt(sale.getCreatedAt())
                .updatedAt(sale.getUpdatedAt());

        // Load thông tin sản phẩm
        enrichWithProductInfo(builder, sale.getProductId());

        return builder.build();
    }

    private void enrichWithProductInfo(SaleDTO.SaleDTOBuilder builder, Long productId) {
        if (productId == null) {
            setDefaultProductInfo(builder);
            return;
        }

        try {
            Optional<Product> productOpt = productService.getProductById(productId);
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                builder
                        .productName(product.getName())
                        .productImageUrl(product.getImageUrl())
                        .originalPrice(product.getPrice())
                        .productDescription(product.getDescription())
                        .categoryName(product.getCategory() != null ?
                                product.getCategory().getCategoryName() : "Chưa phân loại");
            } else {
                setDeletedProductInfo(builder);
            }
        } catch (Exception e) {
            log.warn("Cannot load product info for product ID {}: {}", productId, e.getMessage());
            setErrorProductInfo(builder);
        }
    }

    private void setDefaultProductInfo(SaleDTO.SaleDTOBuilder builder) {
        builder
                .productName("Không có sản phẩm")
                .productImageUrl("/images/default-product.png")
                .originalPrice(null)
                .categoryName("N/A");
    }

    private void setDeletedProductInfo(SaleDTO.SaleDTOBuilder builder) {
        builder
                .productName("Sản phẩm đã bị xóa")
                .productImageUrl("/images/default-product.png")
                .originalPrice(null)
                .categoryName("N/A");
    }

    private void setErrorProductInfo(SaleDTO.SaleDTOBuilder builder) {
        builder
                .productName("Lỗi tải thông tin sản phẩm")
                .productImageUrl("/images/default-product.png")
                .originalPrice(null)
                .categoryName("N/A");
    }

    // ==================== INNER CLASSES ====================

    @lombok.Data
    @lombok.Builder
    public static class SaleStatistics {
        private int totalSales;
        private int activeSales;
        private int expiredSales;
        private int scheduledSales;
        private int inactiveSales;
    }
}