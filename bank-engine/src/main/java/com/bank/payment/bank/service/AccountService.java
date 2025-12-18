package com.bank.payment.bank.service;

import com.bank.payment.bank.client.UserClient;
import com.bank.payment.bank.dto.BalanceModificationRequest;
import com.bank.payment.bank.entity.Account;
import com.bank.payment.bank.exception.AccountNotFoundException;
import com.bank.payment.bank.exception.InsufficientBalanceException;
import com.bank.payment.bank.exception.InvalidAccountException;
import com.bank.payment.bank.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository repository;

    private final UserClient userClient;

    public Account createAccount(Long userId, String username) {
        log.info("Creating account for user: {}", userId);

        Account account = new Account();
        account.setUserId(userId);
        account.setAccountNumber(UUID.randomUUID().toString());
        account.setCurrentBalance(BigDecimal.ZERO);
        account.setCurrency("USD");
        account.setAccountType("SAVINGS");
        account.setAccountStatus("ACTIVE");
        account.setIfscCode("BANK001");
        account.setAddress("Head Office, New York");
        account.setCreatedOn(LocalDateTime.now());
        account.setCreatedBy(username);

        Account savedAccount = repository.save(account);
        log.info("Account created successfully: accountNo={}, userId={}",
                savedAccount.getAccountNumber(), userId);

        updateKyc(userId);

        return savedAccount;
    }

    private void updateKyc(Long userId) {
        try {
            log.debug("Updating KYC status for user: {}", userId);
            userClient.updateKycStatus(userId, true);
            log.info("KYC status updated successfully for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to update KYC for userId={}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to update KYC for userId=" + userId, e);
        }
    }

    public Account getAccountByAccountNo(String accountNo, Long userId) {
        log.debug("Fetching account: accountNo={}, userId={}", accountNo, userId);
        Account account = repository.findByAccountNumber(accountNo)
                .orElseThrow(() -> {
                    log.error("Account not found: {}", accountNo);
                    return new AccountNotFoundException("Account not found");
                });

        if (!account.getUserId().equals(userId)) {
            log.error("Unauthorized access attempt: account={}, userId={}, actualUserId={}",
                    accountNo, userId, account.getUserId());
            throw new InvalidAccountException(
                    "Unauthorized: Account does not belong to user");
        }
        return account;
    }

    @Transactional
    public Account credit(BalanceModificationRequest request) {
        log.info("Crediting account: accountNo={}, amount={}",
                request.getAccountNumber(), request.getAmount());
        Account account = repository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> {
                    log.error("Account not found for credit: {}", request.getAccountNumber());
                    return new AccountNotFoundException("Account not found");
                });
        account.setCurrentBalance(account.getCurrentBalance().add(request.getAmount()));
        Account updated = repository.save(account);
        log.info("Account credited successfully: accountNo={}, newBalance={}",
                request.getAccountNumber(), updated.getCurrentBalance());
        return updated;
    }

    @Transactional
    public Account debit(BalanceModificationRequest request) {
        log.info("Debiting account: accountNo={}, amount={}",
                request.getAccountNumber(), request.getAmount());
        Account account = repository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> {
                    log.error("Account not found for debit: {}", request.getAccountNumber());
                    return new AccountNotFoundException("Account not found");
                });
        if (account.getCurrentBalance().compareTo(request.getAmount()) < 0) {
            log.error("Insufficient balance: accountNo={}, balance={}, requestedAmount={}",
                    request.getAccountNumber(), account.getCurrentBalance(), request.getAmount());
            throw new InsufficientBalanceException(
                    "Insufficient funds");
        }
        account.setCurrentBalance(account.getCurrentBalance().subtract(request.getAmount()));
        Account updated = repository.save(account);
        log.info("Account debited successfully: accountNo={}, newBalance={}",
                request.getAccountNumber(), updated.getCurrentBalance());
        return updated;
    }
}
