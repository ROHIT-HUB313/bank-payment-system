package com.bank.payment.user.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String phoneNumber;
    private String role;
    private boolean kycVerified;
    private String address;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;
    private String updatedBy;
}
