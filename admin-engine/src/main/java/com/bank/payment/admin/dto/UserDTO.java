
package com.bank.payment.admin.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String role;
    private boolean kycVerified;
    private String address;
}
