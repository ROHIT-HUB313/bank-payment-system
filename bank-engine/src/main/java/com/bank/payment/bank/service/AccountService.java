
package com.bank.payment.bank.service;

import com.bank.payment.bank.client.UserClient;
import com.bank.payment.bank.dto.BalanceModificationRequest;
import com.bank.payment.bank.entity.Account;
import com.bank.payment.bank.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository repository;

    private final UserClient userClient;

    @Transactional
    public Account createAccount(Long userId, String username) {

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

        updateKyc(userId);

        return savedAccount;
    }

    private void updateKyc(Long userId) {
        try {
            userClient.updateKycStatus(userId, true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update KYC for userId=" + userId, e);
        }
    }

    public Account getAccountByAccountNo(String accountNo, Long userId) {
        Account account = repository.findByAccountNumber(accountNo)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Account does not belong to user");
        }
        return account;
    }

    @Transactional
    public Account credit(BalanceModificationRequest request) {
        Account account = repository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        account.setCurrentBalance(account.getCurrentBalance().add(request.getAmount()));
        return repository.save(account);
    }

    @Transactional
    public Account debit(BalanceModificationRequest request) {
        Account account = repository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        if (account.getCurrentBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient funds");
        }
        account.setCurrentBalance(account.getCurrentBalance().subtract(request.getAmount()));
        return repository.save(account);
    }
}
