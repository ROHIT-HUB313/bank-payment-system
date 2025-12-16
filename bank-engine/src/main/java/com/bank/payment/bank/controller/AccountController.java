
package com.bank.payment.bank.controller;

import com.bank.payment.bank.dto.BalanceModificationRequest;
import com.bank.payment.bank.entity.Account;
import com.bank.payment.bank.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService service;

    @PostMapping("/public/create")
    public Account createAccount(@RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Name") String username) {
        return service.createAccount(userId, username);
    }

    @GetMapping("/public/user")
    public Account getAccount(@RequestParam String accountNo, @RequestHeader("X-User-Id") Long userId) {
        return service.getAccountByAccountNo(accountNo, userId);
    }

    @PostMapping("/internal/credit")
    public Account credit(@RequestBody BalanceModificationRequest request,
            @RequestHeader("X-Internal-Secret") String secret) {
        if (!"bank-payment-system-internal-secret".equals(secret)) {
            throw new RuntimeException("Unauthorized Access");
        }
        return service.credit(request);
    }

    @PostMapping("/internal/debit")
    public Account debit(@RequestBody BalanceModificationRequest request,
            @RequestHeader("X-Internal-Secret") String secret) {
        if (!"bank-payment-system-internal-secret".equals(secret)) {
            throw new RuntimeException("Unauthorized Access");
        }
        return service.debit(request);
    }
}
