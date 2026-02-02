package com.bank.payment.bank.service;

import com.bank.payment.bank.client.UserClient;
import com.bank.payment.bank.dto.BalanceModificationRequest;
import com.bank.payment.bank.entity.Account;
import com.bank.payment.bank.exception.AccountNotFoundException;
import com.bank.payment.bank.exception.InsufficientBalanceException;
import com.bank.payment.bank.exception.InvalidAccountException;
import com.bank.payment.bank.dto.AccountResponse;
import com.bank.payment.bank.dto.CreateAccountRequest;
import com.bank.payment.bank.enums.AccountStatus;
import com.bank.payment.bank.enums.AccountType;
import com.bank.payment.bank.mapper.AccountMapper;
import com.bank.payment.bank.repository.AccountRepository;
import com.bank.payment.bank.repository.BranchRepository;
import com.bank.payment.bank.entity.Branch;
import com.bank.payment.bank.websocket.BalanceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository repository;
    private final UserClient userClient;
    private final BranchRepository branchRepository;
    private final AccountMapper accountMapper;
    private final BalanceWebSocketHandler webSocketHandler;

    public AccountResponse createAccount(Long userId, String username, CreateAccountRequest request) {
        log.info("Creating account for user: {}, type: {}", userId, request.getAccountType());

        String branchCode = request.getBranchCode() != null ? request.getBranchCode() : "HEAD_OFFICE";

        Branch branch = branchRepository.findByBranchCode(branchCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Branch Code: " + branchCode));

        if (!"INR".equals(request.getCurrency())) {
            throw new IllegalArgumentException("Only INR currency is supported");
        }

        Account account = new Account();
        account.setUserId(userId);

        AccountType type = AccountType.valueOf(request.getAccountType());
        account.setCurrency(request.getCurrency());
        account.setAccountType(type);
        account.setAccountStatus(AccountStatus.ACTIVE);

        account.setAccountNumber(generateAccountNumber());

        account.setCurrentBalance(getInitialBalance(type));

        account.setIfscCode(branch.getIfscCode());
        account.setAddress(branch.getAddress());

        account.setCreatedOn(LocalDateTime.now());
        account.setCreatedBy(username);
        account.setUpdatedBy(username);
        account.setUpdatedOn(LocalDateTime.now());

        Account savedAccount = repository.save(account);
        log.info("Account created successfully: accountNo={}, userId={}, branch={}",
                savedAccount.getAccountNumber(), userId, branchCode);

        updateKyc(userId);

        return accountMapper.toResponse(savedAccount);
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

    public AccountResponse getAccountByAccountNo(String accountNo, Long userId) {
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
        return accountMapper.toResponse(account);
    }

    @Transactional
    public AccountResponse credit(BalanceModificationRequest request) {
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

        webSocketHandler.broadcastBalanceUpdate(
                request.getAccountNumber(),
                updated.getCurrentBalance(),
                "CREDIT");

        return accountMapper.toResponse(updated);
    }

    @Transactional
    public AccountResponse debit(BalanceModificationRequest request) {
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

        webSocketHandler.broadcastBalanceUpdate(
                request.getAccountNumber(),
                updated.getCurrentBalance(),
                "DEBIT");

        return accountMapper.toResponse(updated);
    }

    private String generateAccountNumber() {
        long randomNum = ThreadLocalRandom.current().nextLong(1_000_000_000_000_000L,
                10_000_000_000_000_000L);
        return String.valueOf(randomNum);
    }

    private BigDecimal getInitialBalance(AccountType type) {
        return switch (type) {
            case SAVINGS -> new BigDecimal("500.00");
            case CURRENT -> new BigDecimal("2500.00");
            case BUSINESS -> new BigDecimal("10000.00");
            case SALARY -> BigDecimal.ZERO;
        };
    }
}
