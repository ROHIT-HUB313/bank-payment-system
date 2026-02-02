
package com.bank.payment.user.controller;

import com.bank.payment.user.dto.*;
import com.bank.payment.user.service.AuthService;
import com.bank.payment.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService service;

    private final AuthService authService;

    @PostMapping("/public/register")
    public ResponseEntity<String> addNewUser(@Valid @RequestBody AuthRequest user) {
        log.info("Registration request received for username: {}", user.getUsername());
        String response = service.saveUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/public/token")
    public AuthResponse getToken(@Valid @RequestBody AuthRequest authRequest) {
        log.info("Token request received for username: {}", authRequest.getUsername());
        return authService.authenticateAndGenerateToken(authRequest);
    }

    @GetMapping("/public/validate")
    public String validateToken(@RequestParam("token") String token) {
        log.debug("Token validation request received");
        service.validateToken(token);
        return "Token is valid";
    }

    @GetMapping("/internal/all")
    public List<UserDto> getAllUsers() {
        log.info("Get all users request received");
        return service.getAllUsers();
    }

    @GetMapping("/internal/{id}")
    public UserDto getUserById(@PathVariable Long id) {
        log.info("Get user by ID request received: {}", id);
        return service.getUserById(id);
    }

    @PutMapping("/internal/{id}/kyc")
    public String updateKycStatus(@PathVariable Long id, @RequestParam boolean status) {
        log.info("KYC status update request received for user: {}, status: {}", id, status);
        service.updateKycStatus(id, status);
        return "KYC status updated";
    }

    @PatchMapping("/public/profile")
    public String updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("Profile update request received for user: {}", userId);
        return service.updateProfile(userId, request);
    }

    @PatchMapping("/public/reset-password")
    public String resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Password reset request received for username: {}", request.getUsername());
        return service.resetPassword(request);
    }

    /* Refresh the access token using a valid refresh token.*/
    @PostMapping("/public/refresh-token")
    public AuthResponse refreshToken(@RequestBody RefreshTokenRequest request) {
        log.info("Refresh token request received");
        return authService.refreshAccessToken(request.getRefreshToken());
    }

    /* Logout - Revoke all refresh tokens for the authenticated user.*/
    @PostMapping("/public/logout")
    public ResponseEntity<String> logout(@RequestHeader("X-User-Id") Long userId) {
        log.info("Logout request received for userId: {}", userId);
        authService.logout(userId);
        return ResponseEntity.ok("Logged out successfully");
    }
}
