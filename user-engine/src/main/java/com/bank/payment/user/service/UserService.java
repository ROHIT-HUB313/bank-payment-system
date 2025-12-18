
package com.bank.payment.user.service;

import com.bank.payment.user.dto.AuthRequest;
import com.bank.payment.user.dto.ResetPasswordRequest;
import com.bank.payment.user.dto.UpdateProfileRequest;
import com.bank.payment.user.dto.UserDto;
import com.bank.payment.user.entity.User;
import com.bank.payment.user.exception.DuplicateResourceException;
import com.bank.payment.user.exception.UserNotFoundException;
import com.bank.payment.user.mapper.UserMapper;
import com.bank.payment.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository repository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    public String saveUser(AuthRequest credential) {
        log.info("Registering new user: {}", credential.getUsername());

        User user = new User();
        user.setUsername(credential.getUsername());
        user.setPassword(passwordEncoder.encode(credential.getPassword()));
        user.setEmail(credential.getEmail());
        user.setPhoneNumber(credential.getPhoneNumber());
        user.setRole(credential.getRole() != null && !credential.getRole().isEmpty()
                ? credential.getRole().toUpperCase() : "USER");
        user.setKycVerified(false);
        user.setAddress(credential.getAddress());
        user.setCreatedOn(LocalDateTime.now());
        user.setUpdatedOn(LocalDateTime.now());
        user.setUpdatedBy(credential.getUsername());
        repository.save(user);

        log.info("User registered successfully: {}", credential.getUsername());
        return "User registered successfully";
    }

    public void validateToken(String token) {
        jwtService.validateToken(token);
    }

    public List<UserDto> getAllUsers() {
        return repository.findAll().stream()
                .map(UserMapper::toDto)
                .toList();
    }

    public UserDto getUserById(Long id) {
        log.debug("Fetching user by ID: {}", id);
        User user = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("User not found with id: {}", id);
                    return new UserNotFoundException("User not found with id: " + id);
                });
        return UserMapper.toDto(user);
    }

    public void updateKycStatus(Long userId, boolean status) {
        log.info("Updating KYC status for user {}: {}", userId, status);
        User user = repository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with id: {}", userId);
                    return new UserNotFoundException("User not found with id: " + userId);
                });
        user.setKycVerified(status);
        repository.save(user);
        log.info("KYC status updated successfully for user {}", userId);
    }

    public String updateProfile(Long userId, UpdateProfileRequest request) {
        log.info("Updating profile for user: {}", userId);
        User user = repository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with id: {}", userId);
                    return new UserNotFoundException("User not found");
                });

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (repository.existsByEmail(request.getEmail())) {
                log.error("Email already in use: {}", request.getEmail());
                throw new DuplicateResourceException("Email already in use");
            }
            user.setEmail(request.getEmail());
            log.debug("Email updated for user {}", userId);
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(user.getPhoneNumber())) {
            if (repository.existsByPhoneNumber(request.getPhoneNumber())) {
                log.error("Phone number already in use: {}", request.getPhoneNumber());
                throw new DuplicateResourceException("Phone number already in use");
            }
            user.setPhoneNumber(request.getPhoneNumber());
            log.debug("Phone number updated for user {}", userId);
        }

        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }

        user.setUpdatedOn(LocalDateTime.now());
        user.setUpdatedBy(user.getUsername());

        repository.save(user);
        log.info("Profile updated successfully for user: {}", userId);
        return "Profile updated successfully";
    }

    public String resetPassword(ResetPasswordRequest request) {
        log.info("Password reset requested for user: {}", request.getUsername());
        User user = repository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", request.getUsername());
                    return new UserNotFoundException("User not found with username: " + request.getUsername());
                });

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.error("Password reset failed: incorrect current password for user {}", request.getUsername());
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedOn(LocalDateTime.now());
        user.setUpdatedBy(request.getUsername());

        repository.save(user);
        log.info("Password reset successfully for user: {}", request.getUsername());
        return "Password reset successfully";
    }
}
