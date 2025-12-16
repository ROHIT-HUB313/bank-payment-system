package com.bank.payment.user.service;

import com.bank.payment.user.dto.AuthRequest;
import com.bank.payment.user.dto.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    public AuthResponse authenticateAndGenerateToken(AuthRequest request) {
        Authentication authentication =
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(),
                        request.getPassword()));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // this constructor returns a string, enabled allargsconstructor!
        return new AuthResponse(
                jwtService.generateToken(
                        userDetails.getUserId(),
                        userDetails.getUsername(),
                        userDetails.getRole()
                )
        );
    }

}
