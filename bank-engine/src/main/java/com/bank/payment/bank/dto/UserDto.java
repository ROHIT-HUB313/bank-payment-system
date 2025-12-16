package com.bank.payment.bank.dto;

import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String role;
    private boolean kycVerified;
    private String address;
}
