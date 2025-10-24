package com.example.food.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminDemoController {

    /**
     * Store location setup page - redirect to main store settings
     */
    @GetMapping("/admin/store-location")
    public String storeLocation() {
        return "redirect:/admin/store-settings";
    }
}
