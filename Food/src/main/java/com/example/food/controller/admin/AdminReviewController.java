package com.example.food.controller.admin;

import com.example.food.dto.ApiResponse;
import com.example.food.dto.ReviewActivityDTO;
import com.example.food.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
@Slf4j
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public String page(Model model) {
        model.addAttribute("pageTitle", "Quản lý đánh giá");
        return "admin/reviews/list";
    }

    @GetMapping("/feed")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<ReviewActivityDTO>>> feed(@RequestParam(defaultValue = "20") int size) {
        try {
            List<ReviewActivityDTO> items = reviewService.getRecentActivities(size);
            return ResponseEntity.ok(ApiResponse.<List<ReviewActivityDTO>>builder()
                    .success(true)
                    .message("OK")
                    .data(items)
                    .build());
        } catch (Exception e) {
            log.error("Error fetching review feed", e);
            return ResponseEntity.badRequest().body(ApiResponse.<List<ReviewActivityDTO>>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
}


