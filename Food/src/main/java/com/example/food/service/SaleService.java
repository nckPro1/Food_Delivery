package com.example.food.service;

import com.example.food.dto.CreateSaleRequest;
import com.example.food.dto.SaleDTO;
import com.example.food.model.Product;
import com.example.food.model.Sale;
import com.example.food.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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

    public SaleDTO getSaleById(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Sale not found"));
        return convertToDTO(sale);
    }

    @Transactional
    public SaleDTO createSale(CreateSaleRequest request) {
        // Thêm điều kiện kiểm tra đã có sale active trên sản phẩm chưa
        LocalDateTime now = LocalDateTime.now();
        if (request.getProductId() != null) {
            boolean hasActiveSale = saleRepository
                    .findByProductIdAndIsActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                            request.getProductId(), now, now
                    ).isPresent();
            if (hasActiveSale) {
                throw new RuntimeException("Sản phẩm này đang trong thời gian khuyến mãi. Không thể tạo thêm sale mới.");
            }
        }
        Sale sale = Sale.builder()
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

        Sale savedSale = saleRepository.save(sale);

        // Update product sale fields
        updateProductSaleFields(savedSale);

        return convertToDTO(savedSale);
    }

    @Transactional
    public SaleDTO updateSale(Long saleId, CreateSaleRequest request) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Sale not found"));

        sale.setSaleName(request.getSaleName());
        sale.setSaleDescription(request.getSaleDescription());
        sale.setDiscountType(Sale.DiscountType.valueOf(request.getDiscountType()));
        sale.setDiscountValue(request.getDiscountValue());
        sale.setSalePrice(request.getSalePrice());
        sale.setStartDate(request.getStartDate());
        sale.setEndDate(request.getEndDate());

        Sale savedSale = saleRepository.save(sale);

        // Update product sale fields
        updateProductSaleFields(savedSale);

        return convertToDTO(savedSale);
    }

    @Transactional
    public void deleteSale(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Sale not found"));

        saleRepository.delete(sale);

        // Clear product sale fields
        clearProductSaleFields(sale.getProductId());
    }

    @Transactional
    public SaleDTO toggleSaleStatus(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Sale not found"));

        sale.setIsActive(!sale.getIsActive());
        Sale savedSale = saleRepository.save(sale);

        // Update product sale fields
        if (savedSale.getIsActive()) {
            updateProductSaleFields(savedSale);
        } else {
            clearProductSaleFields(savedSale.getProductId());
        }

        return convertToDTO(savedSale);
    }

    /**
     * Deactivate expired sales (sales that have passed their endDate)
     */
    @Transactional
    public int deactivateExpiredSales() {
        LocalDateTime now = LocalDateTime.now();
        List<Sale> expiredSales = saleRepository.findAll().stream()
                .filter(s -> s.getIsActive() && s.getEndDate() != null && now.isAfter(s.getEndDate()))
                .collect(Collectors.toList());

        for (Sale sale : expiredSales) {
            sale.setIsActive(false);
            // Clear product sale fields for expired sales
            clearProductSaleFields(sale.getProductId());
        }

        saleRepository.saveAll(expiredSales);
        return expiredSales.size();
    }

    /**
     * Cleanup old expired sales (delete sales that have been expired for more than a certain period)
     * This method deletes sales that ended more than 30 days ago
     */
    @Transactional
    public int cleanupOldExpiredSales() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        List<Sale> oldExpiredSales = saleRepository.findAll().stream()
                .filter(s -> !s.getIsActive() && s.getEndDate() != null && s.getEndDate().isBefore(cutoffDate))
                .collect(Collectors.toList());

        saleRepository.deleteAll(oldExpiredSales);
        return oldExpiredSales.size();
    }

    /**
     * Get sale statistics
     */
    public SaleStatistics getSaleStatistics() {
        List<Sale> allSales = saleRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        long totalSales = allSales.size();
        long activeSales = allSales.stream()
                .filter(s -> s.getIsActive() && s.getEndDate() != null && now.isBefore(s.getEndDate()))
                .count();
        long expiredSales = allSales.stream()
                .filter(s -> !s.getIsActive() || (s.getEndDate() != null && now.isAfter(s.getEndDate())))
                .count();

        return new SaleStatistics(totalSales, activeSales, expiredSales);
    }

    /**
     * Inner class for sale statistics
     */
    public static class SaleStatistics {
        private final long totalSales;
        private final long activeSales;
        private final long expiredSales;

        public SaleStatistics(long totalSales, long activeSales, long expiredSales) {
            this.totalSales = totalSales;
            this.activeSales = activeSales;
            this.expiredSales = expiredSales;
        }

        public long getTotalSales() {
            return totalSales;
        }

        public long getActiveSales() {
            return activeSales;
        }

        public long getExpiredSales() {
            return expiredSales;
        }
    }

    private void updateProductSaleFields(Sale sale) {
        Product product = productService.getProductById(sale.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setIsOnSale(true);
        product.setSalePrice(sale.getSalePrice());
        product.setSalePercentage(sale.getDiscountType() == Sale.DiscountType.PERCENTAGE ?
                sale.getDiscountValue().intValue() : null);
        product.setSaleStartDate(sale.getStartDate());
        product.setSaleEndDate(sale.getEndDate());

        productService.updateProduct(product.getProductId(), product);
    }

    private void clearProductSaleFields(Long productId) {
        Product product = productService.getProductById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setIsOnSale(false);
        product.setSalePrice(null);
        product.setSalePercentage(null);
        product.setSaleStartDate(null);
        product.setSaleEndDate(null);

        productService.updateProduct(product.getProductId(), product);
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

        // Populate product information if productId exists
        if (sale.getProductId() != null) {
            try {
                Product product = productService.getProductById(sale.getProductId()).orElse(null);
                if (product != null) {
                    builder.productName(product.getName())
                            .productImageUrl(product.getImageUrl())
                            .originalPrice(product.getPrice()); // Giá gốc của sản phẩm

                    // Get category name if category exists
                    if (product.getCategory() != null) {
                        builder.categoryName(product.getCategory().getCategoryName());
                    }
                }
            } catch (Exception e) {
                // Log error but don't fail the DTO conversion
                // Product might not exist or there might be other issues
            }
        }

        return builder.build();
    }
}
