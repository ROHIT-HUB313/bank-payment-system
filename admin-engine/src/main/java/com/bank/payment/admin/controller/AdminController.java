package com.bank.payment.admin.controller;

import com.bank.payment.admin.dto.TransactionDTO;
import com.bank.payment.admin.dto.UserDTO;
import com.bank.payment.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService service;

    @GetMapping("/users")
    public List<UserDTO> getUsers(@RequestHeader("X-User-Role") String role) {
        log.info("Admin request: Get all users, role={}", role);
        return service.getAllUsers(role);
    }

    @GetMapping("/transactions")
    public List<TransactionDTO> getTransactions(@RequestHeader("X-User-Role") String role) {
        log.info("Admin request: Get all transactions, role={}", role);
        return service.getAllTransactions(role);
    }
}
