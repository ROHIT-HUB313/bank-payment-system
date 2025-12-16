package com.bank.payment.transaction.controller;

import com.bank.payment.transaction.dto.BalanceModificationRequest;
import com.bank.payment.transaction.dto.TransactionRequest;
import com.bank.payment.transaction.entity.Transaction;
import com.bank.payment.transaction.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;

    @PostMapping("/public/transfer")
    public Transaction transfer(@RequestBody TransactionRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return service.transfer(request, userId);
    }

    @PostMapping("/public/deposit")
    public Transaction deposit(@RequestBody BalanceModificationRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return service.deposit(request, userId);
    }

    @PostMapping("/public/withdraw")
    public Transaction withdraw(@RequestBody BalanceModificationRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return service.withdraw(request, userId);
    }

    @GetMapping("/internal/all")
    public java.util.List<Transaction> getAllTransactions(@RequestHeader("X-Internal-Secret") String secret) {
        if (!"bank-payment-system-internal-secret".equals(secret)) {
            throw new RuntimeException("Unauthorized Access");
        }
        return service.getAllTransactions();
    }
}
