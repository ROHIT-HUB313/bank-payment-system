
package com.bank.payment.user.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
    private String email; // Optional for login
    private String address; // Optional for login
    private String role; // Optional for registration (default USER)
}
