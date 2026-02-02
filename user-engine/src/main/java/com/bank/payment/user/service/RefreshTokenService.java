package com.bank.payment.user.service;

import com.bank.payment.user.entity.RefreshToken;
import com.bank.payment.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    // 7 days in seconds
    private static final long REFRESH_TOKEN_VALIDITY = 7 * 24 * 60 * 60;

    private final RefreshTokenRepository refreshTokenRepository;

    /* Create a new refresh token for a user. Revokes any existing refresh tokens for the same user. */
    @Transactional
    public RefreshToken createRefreshToken(Long userId, String username, String role) {
        log.info("Creating refresh token for user: {} with role: {}", username, role);

        // Revoke any existing refresh tokens for this user
        refreshTokenRepository.revokeAllByUserId(userId);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(userId)
                .username(username)
                .role(role)
                .expiryDate(Instant.now().plusSeconds(REFRESH_TOKEN_VALIDITY))
                .revoked(false)
                .build();

        refreshToken = refreshTokenRepository.save(refreshToken);
        log.info("Refresh token created for user: {}, role: {}, expires at: {}",
                username, role, refreshToken.getExpiryDate());

        return refreshToken;
    }

    /* Validate a refresh token. Returns the token if valid, throws exception otherwise. */
    public RefreshToken validateRefreshToken(String token) {
        log.info("Validating refresh token");

        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(token);

        if (refreshTokenOpt.isEmpty()) {
            log.error("Refresh token not found");
            throw new RuntimeException("Invalid refresh token");
        }

        RefreshToken refreshToken = refreshTokenOpt.get();

        if (refreshToken.isRevoked()) {
            log.error("Refresh token is revoked for user: {}", refreshToken.getUsername());
            throw new RuntimeException("Refresh token has been revoked");
        }

        if (refreshToken.isExpired()) {
            log.error("Refresh token expired for user: {}", refreshToken.getUsername());
            throw new RuntimeException("Refresh token has expired");
        }

        log.info("Refresh token validated for user: {}", refreshToken.getUsername());
        return refreshToken;
    }

    /* Revoke a refresh token */
    @Transactional
    public void revokeRefreshToken(String token) {
        log.info("Revoking refresh token");
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
            log.info("Refresh token revoked for user: {}", rt.getUsername());
        });
    }

    /* Revoke all refresh tokens for a user (e.g., on logout from all devices) */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        log.info("Revoking all refresh tokens for userId: {}", userId);
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}
