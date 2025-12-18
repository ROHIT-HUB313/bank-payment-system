
package com.bank.payment.bank.controller;

import com.bank.payment.bank.dto.BalanceModificationRequest;
import com.bank.payment.bank.entity.Account;
import com.bank.payment.bank.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService service;

    @PostMapping("/public/create")
    public Account createAccount(@RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Name") String username) {
        log.info("Account creation request received for user: {}", userId);
        return service.createAccount(userId, username);
    }

    @GetMapping("/public/user")
    public Account getAccount(@RequestParam String accountNo, @RequestHeader("X-User-Id") Long userId) {
        log.info("Get account request received: accountNo={}, userId={}", accountNo, userId);
        return service.getAccountByAccountNo(accountNo, userId);
    }

    @PostMapping("/internal/credit")
    public Account credit(@Valid @RequestBody BalanceModificationRequest request,
            @RequestHeader("X-Internal-Secret") String secret) {
        log.info("Credit request received (internal): account={}, amount={}",
                request.getAccountNumber(), request.getAmount());
        if (!"bank-payment-system-internal-secret".equals(secret)) {
            log.error("Unauthorized access attempt to internal credit endpoint");
            throw new RuntimeException("Unauthorized Access");
        }
        return service.credit(request);
    }

    @PostMapping("/internal/debit")
    public Account debit(@Valid @RequestBody BalanceModificationRequest request,
            @RequestHeader("X-Internal-Secret") String secret) {
        log.info("Debit request received (internal): account={}, amount={}",
                request.getAccountNumber(), request.getAmount());
        if (!"bank-payment-system-internal-secret".equals(secret)) {
            log.error("Unauthorized access attempt to internal debit endpoint");
            throw new RuntimeException("Unauthorized Access");
        }
        return service.debit(request);
    }
}
