package com.example.food.controller.admin;

import com.example.food.dto.ApiResponse;
import com.example.food.dto.ShippingFeeDTO;
import com.example.food.service.ShippingFeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/shipping-fees")
@RequiredArgsConstructor
@Slf4j
public class AdminShippingFeeController {

    private final ShippingFeeService shippingFeeService;

    /**
     * Lấy tất cả shipping fees
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ShippingFeeDTO>>> getAllShippingFees() {
        try {
            List<ShippingFeeDTO> fees = shippingFeeService.getAllShippingFees();
            return ResponseEntity.ok(ApiResponse.<List<ShippingFeeDTO>>builder()
                    .success(true)
                    .message("Lấy danh sách phí ship thành công")
                    .data(fees)
                    .build());
        } catch (Exception e) {
            log.error("Error getting shipping fees: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<List<ShippingFeeDTO>>builder()
                    .success(false)
                    .message("Lỗi khi lấy danh sách phí ship: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Lấy shipping fee theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShippingFeeDTO>> getShippingFeeById(@PathVariable Long id) {
        try {
            return shippingFeeService.getShippingFeeById(id)
                    .map(fee -> ResponseEntity.ok(ApiResponse.<ShippingFeeDTO>builder()
                            .success(true)
                            .message("Lấy thông tin phí ship thành công")
                            .data(fee)
                            .build()))
                    .orElse(ResponseEntity.badRequest().body(ApiResponse.<ShippingFeeDTO>builder()
                            .success(false)
                            .message("Không tìm thấy phí ship với ID: " + id)
                            .build()));
        } catch (Exception e) {
            log.error("Error getting shipping fee by ID: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<ShippingFeeDTO>builder()
                    .success(false)
                    .message("Lỗi khi lấy thông tin phí ship: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Tạo shipping fee mới
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ShippingFeeDTO>> createShippingFee(@RequestBody ShippingFeeDTO shippingFeeDTO) {
        try {
            ShippingFeeDTO created = shippingFeeService.createShippingFee(shippingFeeDTO);
            return ResponseEntity.ok(ApiResponse.<ShippingFeeDTO>builder()
                    .success(true)
                    .message("Tạo phí ship thành công")
                    .data(created)
                    .build());
        } catch (Exception e) {
            log.error("Error creating shipping fee: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<ShippingFeeDTO>builder()
                    .success(false)
                    .message("Lỗi khi tạo phí ship: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Cập nhật shipping fee
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ShippingFeeDTO>> updateShippingFee(
            @PathVariable Long id, 
            @RequestBody ShippingFeeDTO shippingFeeDTO) {
        try {
            ShippingFeeDTO updated = shippingFeeService.updateShippingFee(id, shippingFeeDTO);
            return ResponseEntity.ok(ApiResponse.<ShippingFeeDTO>builder()
                    .success(true)
                    .message("Cập nhật phí ship thành công")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Error updating shipping fee: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<ShippingFeeDTO>builder()
                    .success(false)
                    .message("Lỗi khi cập nhật phí ship: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Xóa shipping fee (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteShippingFee(@PathVariable Long id) {
        try {
            shippingFeeService.deleteShippingFee(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Xóa phí ship thành công")
                    .build());
        } catch (Exception e) {
            log.error("Error deleting shipping fee: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Lỗi khi xóa phí ship: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Đặt làm shipping fee mặc định
     */
    @PostMapping("/{id}/set-default")
    public ResponseEntity<ApiResponse<Void>> setDefaultShippingFee(@PathVariable Long id) {
        try {
            shippingFeeService.setDefaultShippingFee(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Đặt phí ship mặc định thành công")
                    .build());
        } catch (Exception e) {
            log.error("Error setting default shipping fee: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Lỗi khi đặt phí ship mặc định: " + e.getMessage())
                    .build());
        }
    }
}