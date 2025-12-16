
package com.bank.payment.user.service;

import com.bank.payment.user.dto.AuthRequest;
import com.bank.payment.user.dto.UserDto;
import com.bank.payment.user.entity.User;
import com.bank.payment.user.mapper.UserMapper;
import com.bank.payment.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    public String saveUser(AuthRequest credential) {
        User user = new User();
        user.setUsername(credential.getUsername());
        user.setEmail(credential.getEmail());
        user.setPassword(passwordEncoder.encode(credential.getPassword()));
        user.setRole(credential.getRole() != null && !credential.getRole().isEmpty() ? credential.getRole().toUpperCase() : "USER");
        user.setKycVerified(false);
        user.setAddress(credential.getAddress());
        repository.save(user);
        return "user added to the system";
    }

    public void validateToken(String token) {
        jwtService.validateToken(token);
    }

    public java.util.List<UserDto> getAllUsers() {
        return repository.findAll().stream()
                .map(UserMapper::toDto)
                .toList();
    }

    public UserDto getUserById(Long id) {
        User user = repository.findById(id).orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return UserMapper.toDto(user);
    }

    public void updateKycStatus(Long userId, boolean status) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        user.setKycVerified(status);
        repository.save(user);
    }
}
