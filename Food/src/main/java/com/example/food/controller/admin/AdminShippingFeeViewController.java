package com.example.food.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/shipping-fees")
public class AdminShippingFeeViewController {

    /**
     * Hiển thị trang quản lý shipping fees
     */
    @GetMapping("/list")
    public String shippingFeesList() {
        return "admin/shipping-fees/list";
    }
    
    /**
     * Redirect từ /admin/shipping-fees/view đến /admin/shipping-fees/list
     */
    @GetMapping("/view")
    public String shippingFeesRedirect() {
        return "redirect:/admin/shipping-fees/list";
    }
}
