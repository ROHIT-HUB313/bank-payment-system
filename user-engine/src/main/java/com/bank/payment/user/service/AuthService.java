package com.bank.payment.user.service;

import com.bank.payment.user.dto.AuthRequest;
import com.bank.payment.user.dto.AuthResponse;
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

    public AuthResponse authenticateAndGenerateToken(AuthRequest request) {
        log.info("Authentication attempt for user: {}", request.getUsername());

        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(),
                        request.getPassword()));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        log.info("Authentication successful for user: {}, role: {}", request.getUsername(),
                userDetails.getRole());

        // this constructor returns a string, enabled allargsconstructor!
        return new AuthResponse(
                jwtService.generateToken(
                        userDetails.getUserId(),
                        userDetails.getUsername(),
                        userDetails.getRole()));
    }

}
