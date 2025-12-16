
package com.bank.payment.user.controller;

import com.bank.payment.user.dto.AuthRequest;
import com.bank.payment.user.dto.AuthResponse;
import com.bank.payment.user.dto.UserDto;
import com.bank.payment.user.service.AuthService;
import com.bank.payment.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    private final AuthService authService;

    @PostMapping("/public/register")
    public String addNewUser(@RequestBody AuthRequest user) {
        return service.saveUser(user);
    }

    @PostMapping("/public/token")
    public AuthResponse getToken(@RequestBody AuthRequest authRequest) {
        return authService.authenticateAndGenerateToken(authRequest);
    }

    @GetMapping("/public/validate")
    public String validateToken(@RequestParam("token") String token) {
        service.validateToken(token);
        return "Token is valid";
    }

    @GetMapping("/internal/all")
    public java.util.List<UserDto> getAllUsers() {
        return service.getAllUsers();
    }

    @GetMapping("/internal/{id}")
    public UserDto getUserById(@PathVariable Long id) {
        return service.getUserById(id);
    }

    @PutMapping("/internal/{id}/kyc")
    public String updateKycStatus(@PathVariable Long id, @RequestParam boolean status) {
        service.updateKycStatus(id, status);
        return "KYC status updated";
    }
}
