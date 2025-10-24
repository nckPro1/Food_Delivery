package com.example.food.dto;

import com.example.food.model.AuthProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long userId;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String address;
    private String avatarUrl;
    private Integer roleId;
    private AuthProvider authProvider;
    private String userCity;
    private String userDistrict;
    private String userWard;
    private String userStreet;
}

