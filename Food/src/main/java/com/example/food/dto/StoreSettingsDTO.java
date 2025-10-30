package com.example.food.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreSettingsDTO {

    private Long id;
    private String storeName;        // Tên cửa hàng
    private String phoneNumber;     // Số điện thoại
    private String email;           // Email
    private String address;         // Địa chỉ
    private String description;     // Giới thiệu
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
