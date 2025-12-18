package com.bank.payment.transaction.controller;

import com.bank.payment.transaction.dto.BalanceModificationRequest;
import com.bank.payment.transaction.dto.TransactionRequest;
import com.bank.payment.transaction.entity.Transaction;
import com.bank.payment.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService service;

    @PostMapping("/public/transfer")
    public Transaction transfer(@Valid @RequestBody TransactionRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("Transfer request received: {} -> {}, amount: {}",
                request.getSenderAccountNumber(),
                request.getReceiverAccountNumber(),
                request.getAmount());
        return service.transfer(request, userId);
    }

    @PostMapping("/public/deposit")
    public Transaction deposit(@Valid @RequestBody BalanceModificationRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("Deposit request received: account={}, amount={}",
                request.getAccountNumber(),
                request.getAmount());
        return service.deposit(request, userId);
    }

    @PostMapping("/public/withdraw")
    public Transaction withdraw(@Valid @RequestBody BalanceModificationRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("Withdrawal request received: account={}, amount={}",
                request.getAccountNumber(),
                request.getAmount());
        return service.withdraw(request, userId);
    }

    @GetMapping("/internal/all")
    public java.util.List<Transaction> getAllTransactions(@RequestHeader("X-Internal-Secret") String secret) {
        log.info("Get all transactions request received (internal)");
        if (!"bank-payment-system-internal-secret".equals(secret)) {
            log.error("Unauthorized access attempt to internal endpoint");
            throw new RuntimeException("Unauthorized Access");
        }
        return service.getAllTransactions();
    }
}
