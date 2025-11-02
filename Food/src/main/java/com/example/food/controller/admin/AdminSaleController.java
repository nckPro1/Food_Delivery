package com.example.food.controller.admin;

import com.example.food.dto.CreateSaleRequest;
import com.example.food.dto.SaleDTO;
import com.example.food.service.ProductService;
import com.example.food.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminSaleController {

    private final SaleService saleService;
    private final ProductService productService;

    // ===============================
    // SALE MANAGEMENT PAGES
    // ===============================

    @GetMapping("/sales")
    public String salesList(Model model) {
        List<SaleDTO> sales = saleService.getAllSales();
        model.addAttribute("sales", sales);
        model.addAttribute("pageTitle", "Sale - Danh sách");
        return "admin/sales/list";
    }

    @GetMapping("/sales/add")
    public String createSaleForm(Model model) {
        model.addAttribute("sale", new CreateSaleRequest());
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("categories", productService.getAllActiveCategories());
        model.addAttribute("pageTitle", "Sale - Tạo mới");
        return "admin/sales/form";
    }

    @PostMapping("/sales/create")
    public String createSale(@ModelAttribute CreateSaleRequest request, RedirectAttributes redirectAttributes) {
        try {
            // Check if this is an update (has saleId) or create (no saleId)
            if (request.getSaleId() != null && request.getSaleId() > 0) {
                saleService.updateSale(request.getSaleId(), request);
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật sale thành công!");
            } else {
                saleService.createSale(request);
                redirectAttributes.addFlashAttribute("successMessage", "Tạo sale thành công!");
            }
            return "redirect:/admin/sales";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tạo sale: " + e.getMessage());
            return "redirect:/admin/sales/add";
        }
    }

    @GetMapping("/sales/{saleId}/edit")
    public String editSaleForm(@PathVariable Long saleId, Model model) {
        SaleDTO saleDTO = saleService.getSaleById(saleId);

        // Convert SaleDTO to CreateSaleRequest for editing
        CreateSaleRequest sale = CreateSaleRequest.builder()
                .saleId(saleDTO.getSaleId())
                .productId(saleDTO.getProductId())
                .saleName(saleDTO.getSaleName())
                .saleDescription(saleDTO.getSaleDescription())
                .discountType(saleDTO.getDiscountType())
                .discountValue(saleDTO.getDiscountValue())
                .salePrice(saleDTO.getSalePrice())
                .startDate(saleDTO.getStartDate())
                .endDate(saleDTO.getEndDate())
                .build();

        model.addAttribute("sale", sale);
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("categories", productService.getAllActiveCategories());
        model.addAttribute("pageTitle", "Sale - Chỉnh sửa");
        return "admin/sales/form";
    }

    @PostMapping("/sales/{saleId}/edit")
    public String updateSale(@PathVariable Long saleId, @ModelAttribute CreateSaleRequest request, RedirectAttributes redirectAttributes) {
        try {
            saleService.updateSale(saleId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật sale thành công!");
            return "redirect:/admin/sales";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật sale: " + e.getMessage());
            return "redirect:/admin/sales/" + saleId + "/edit";
        }
    }

    @PostMapping("/sales/{saleId}/delete")
    public String deleteSale(@PathVariable Long saleId, RedirectAttributes redirectAttributes) {
        try {
            saleService.deleteSale(saleId);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa sale thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xóa sale: " + e.getMessage());
        }
        return "redirect:/admin/sales";
    }

    @PostMapping("/sales/{saleId}/toggle")
    public String toggleSaleStatus(@PathVariable Long saleId, RedirectAttributes redirectAttributes) {
        try {
            saleService.toggleSaleStatus(saleId);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái sale thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật trạng thái sale: " + e.getMessage());
        }
        return "redirect:/admin/sales";
    }

}