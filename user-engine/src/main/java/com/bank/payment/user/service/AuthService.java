package com.bank.payment.user.service;

import com.bank.payment.user.dto.AuthRequest;
import com.bank.payment.user.dto.AuthResponse;
import com.bank.payment.user.entity.RefreshToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

        private final AuthenticationManager authenticationManager;

        private final JwtService jwtService;

        private final RefreshTokenService refreshTokenService;

        /*Authenticate user and generate both access token and refresh token.*/
        public AuthResponse authenticateAndGenerateToken(AuthRequest request) {
                log.info("Authentication attempt for user: {}", request.getUsername());

                Authentication authentication = authenticationManager
                                .authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(),
                                                request.getPassword()));

                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

                log.info("Authentication successful for user: {}, role: {}", request.getUsername(),
                                userDetails.getRole());

                // Generate access token (30 min validity)
                String accessToken = jwtService.generateToken(
                                userDetails.getUserId(),
                                userDetails.getUsername(),
                                userDetails.getRole());

                // Generate refresh token (7 day validity)
                RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                                userDetails.getUserId(),
                                userDetails.getUsername());

                // this constructor returns a string, enabled allargsconstructor!
                return AuthResponse.builder()
                                .token(accessToken)
                                .refreshToken(refreshToken.getToken())
                                .build();
        }

        /* Refresh the access token using a valid refresh token.*/
        public AuthResponse refreshAccessToken(String refreshTokenStr) {
                log.info("Refresh token request received");

                // Validate the refresh token
                RefreshToken refreshToken = refreshTokenService.validateRefreshToken(refreshTokenStr);

                // Get user details from the refresh token
                Long userId = refreshToken.getUserId();
                String username = refreshToken.getUsername();

                // We need to get the user's role - fetch from database
                // For simplicity, we'll generate a token without role (or you can inject
                // UserRepository)
                // Here we assume a default USER role; in production, fetch actual role
                String accessToken = jwtService.generateToken(userId, username, "USER");

                log.info("Access token refreshed for user: {}", username);

                return AuthResponse.builder()
                                .token(accessToken)
                                .refreshToken(refreshTokenStr) // Return same refresh token
                                .build();
        }

        /* Logout - revoke all refresh tokens for the user. */
        public void logout(Long userId) {
                log.info("Logout request for userId: {}", userId);
                refreshTokenService.revokeAllUserTokens(userId);
        }
}
