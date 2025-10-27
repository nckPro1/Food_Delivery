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
        return SaleDTO.builder()
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
                .updatedAt(sale.getUpdatedAt())
                .build();
    }
}
