package com.bank.payment.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 5, message = "Username must be at least 5 characters long")
    private String username;

    /*@Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$",
            message = "Password must be at least 8 characters with uppercase, lowercase, number, and special character")*/
    @NotBlank(message = "Password is required")
    private String password;

    @Email(message = "Invalid email format")
    private String email; // Optional for login, required for registration

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    private String phoneNumber; // Optional for login, required for registration

    private String address; // Optional for login, required for registration

    private String role; // Optional for registration (default USER)
}
