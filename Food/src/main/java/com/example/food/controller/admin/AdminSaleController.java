package com.example.food.controller.admin;

import com.example.food.dto.CreateSaleRequest;
import com.example.food.dto.SaleDTO;
import com.example.food.model.Product;
import com.example.food.model.Sale;
import com.example.food.service.ProductService;
import com.example.food.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/sales")
@RequiredArgsConstructor
public class AdminSaleController {

    private final SaleService saleService;
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<SaleDTO>> getAllSales() {
        List<SaleDTO> sales = saleService.getAllSales();
        return ResponseEntity.ok(sales);
    }

    @GetMapping("/active")
    public ResponseEntity<List<SaleDTO>> getActiveSales() {
        List<SaleDTO> sales = saleService.getActiveSales();
        return ResponseEntity.ok(sales);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<SaleDTO>> getSalesByProduct(@PathVariable Long productId) {
        List<SaleDTO> sales = saleService.getSalesByProduct(productId);
        return ResponseEntity.ok(sales);
    }

    @PostMapping
    public ResponseEntity<SaleDTO> createSale(@RequestBody CreateSaleRequest request) {
        // Validate product exists
        Product product = productService.getProductById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        SaleDTO sale = saleService.createSale(request);
        return ResponseEntity.ok(sale);
    }

    @PutMapping("/{saleId}")
    public ResponseEntity<SaleDTO> updateSale(@PathVariable Long saleId, @RequestBody CreateSaleRequest request) {
        SaleDTO sale = saleService.updateSale(saleId, request);
        return ResponseEntity.ok(sale);
    }

    @DeleteMapping("/{saleId}")
    public ResponseEntity<Void> deleteSale(@PathVariable Long saleId) {
        saleService.deleteSale(saleId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{saleId}/toggle")
    public ResponseEntity<SaleDTO> toggleSaleStatus(@PathVariable Long saleId) {
        SaleDTO sale = saleService.toggleSaleStatus(saleId);
        return ResponseEntity.ok(sale);
    }
}
