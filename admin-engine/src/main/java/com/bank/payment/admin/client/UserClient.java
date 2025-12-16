package com.bank.payment.admin.client;

import com.bank.payment.admin.config.FeignConfig;
import com.bank.payment.admin.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@FeignClient(name = "user-engine", configuration = FeignConfig.class)
public interface UserClient {
    @GetMapping("/users/internal/all")
    List<UserDTO> getAllUsers();
}
