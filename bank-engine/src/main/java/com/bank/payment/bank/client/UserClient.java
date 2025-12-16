package com.bank.payment.bank.client;

import com.bank.payment.bank.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bank.payment.bank.config.FeignConfig;

@FeignClient(name = "user-engine", configuration = FeignConfig.class)
public interface UserClient {
    @GetMapping("/users/internal/{id}")
    UserDto getUserById(@PathVariable("id") Long id);

    @PutMapping("/users/internal/{id}/kyc")
    void updateKycStatus(@PathVariable("id") Long id, @RequestParam("status") boolean status);
}
