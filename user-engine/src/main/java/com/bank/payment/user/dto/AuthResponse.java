package com.bank.payment.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication response containing access token and refresh token.
 * - accessToken: Short-lived JWT (30 minutes)
 * - refreshToken: Long-lived token (7 days) for getting new access tokens
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse {
    private String token; // Access token (backward compatible)
    private String refreshToken; // New: Refresh token for token renewal
}
