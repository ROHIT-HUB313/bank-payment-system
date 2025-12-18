package com.bank.payment.transaction.service;

import com.bank.payment.transaction.client.BankClient;
import com.bank.payment.transaction.dto.AccountDto;
import com.bank.payment.transaction.dto.BalanceModificationRequest;
import com.bank.payment.transaction.dto.TransactionRequest;
import com.bank.payment.transaction.entity.Transaction;
import com.bank.payment.transaction.entity.TransactionStatus;
import com.bank.payment.transaction.entity.TransactionType;
import com.bank.payment.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

// NOTE: This service uses synchronous Feign calls.
// Database transactions do NOT roll back remote bank-engine state.
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository repository;

    private final BankClient bankClient;

    /**
     * Executes a banking-correct transfer with strict idempotency, partial retry
     * support, and ledger integrity.
     */
    public Transaction transfer(TransactionRequest request, Long userId) {
        log.info("Transfer initiated: {} -> {}, amount: {}, key: {}",
                request.getSenderAccountNumber(),
                request.getReceiverAccountNumber(),
                request.getAmount(),
                request.getIdempotencyKey());

        // 0. Security Check: Ownership
        AccountDto senderAccount = bankClient.getAccount(request.getSenderAccountNumber());
        if (!senderAccount.getUserId().equals(userId)) {
            log.error("Transfer authorization failed: sender account {} does not belong to user {}",
                    request.getSenderAccountNumber(), userId);
            throw new RuntimeException("Unauthorized: Sender account does not belong to user");
        }

        // 1. Idempotency Check & State Recovery
        Optional<Transaction> existingDebit = repository.findByIdempotencyKeyAndAccountNumberAndTransactionType(
                request.getIdempotencyKey(),
                request.getSenderAccountNumber(),
                TransactionType.DEBIT);

        Optional<Transaction> existingCredit = repository.findByIdempotencyKeyAndAccountNumberAndTransactionType(
                request.getIdempotencyKey(),
                request.getReceiverAccountNumber(),
                TransactionType.CREDIT);

        String utr;
        Transaction debitTx;

        // --- PHASE 1: SENDER (DEBIT) ---
        if (existingDebit.isPresent()) {
            debitTx = existingDebit.get();
            utr = debitTx.getUtr(); // Reuse existing UTR

            if (debitTx.getStatus() == TransactionStatus.FAILED) {
                // Retry Debit
                debitTx = processDebit(debitTx.getUtr(), request);
            }
            // If SUCCESS, we proceed to PHASE 2
        } else {
            // New Transaction
            utr = UUID.randomUUID().toString();
            debitTx = processDebit(utr, request);
        }

        // If Debit Failed (even after retry), stop here.
        if (debitTx.getStatus() != TransactionStatus.SUCCESS) {
            log.error("Transfer failed at DEBIT phase: key={}, status={}, utr={}",
                    request.getIdempotencyKey(),
                    debitTx.getStatus(),
                    debitTx.getUtr());
            return debitTx;
            // In a real system, we might throw an exception,
            // but returning the FAILED record gives the caller the state.
        }

        // --- PHASE 2: RECEIVER (CREDIT) ---
        Transaction creditTx;
        if (existingCredit.isPresent()) {
            creditTx = existingCredit.get();
            if (creditTx.getStatus() == TransactionStatus.FAILED) {
                // Retry Credit
                creditTx = processCredit(utr, request);
            }
        } else {
            // New Credit Leg
            creditTx = processCredit(utr, request);
        }

        // --- PHASE 3: COMPENSATION (If Credit Failed) ---
        if (creditTx.getStatus() != TransactionStatus.SUCCESS) {
            log.error("Transfer failed at CREDIT phase, initiating reversal: key={}, status={}, utr={}",
                    request.getIdempotencyKey(),
                    creditTx.getStatus(),
                    creditTx.getUtr());
            compensateDebit(debitTx, request.getIdempotencyKey());
            // We return the primary Debit record, but its associated Credit failed and was
            // reversed.
            // Documentation: "On failure → mark CREDIT FAILED Also create a REVERSAL"
            debitTx.setStatus(TransactionStatus.REVERSED);
            repository.save(debitTx);
            return debitTx;
        }

        log.info("Transfer completed successfully: UTR={}, key={}",
                debitTx.getUtr(),
                request.getIdempotencyKey());
        return debitTx;
    }

    @Transactional
    public Transaction processDebit(String utr, TransactionRequest request) {
        // 1️⃣ Claim idempotency FIRST
        Transaction tx = createInitiatedOrReturnExisting(
                utr,
                request.getSenderAccountNumber(),
                request.getAmount(),
                TransactionType.DEBIT,
                request.getIdempotencyKey());

        // Already processed → never call bank again
        if (tx.getStatus() != TransactionStatus.INITIATED) {
            return tx;
        }

        try {
            // 2️⃣ Safe remote call (only one thread reaches here)
            AccountDto updatedAccount = bankClient.debit(
                    BalanceModificationRequest.builder()
                            .accountNumber(request.getSenderAccountNumber())
                            .amount(request.getAmount())
                            .build());

            return createOrUpdateEntry(
                    utr,
                    request.getSenderAccountNumber(),
                    request.getAmount(),
                    TransactionType.DEBIT,
                    TransactionStatus.SUCCESS,
                    request.getIdempotencyKey(),
                    updatedAccount.getCurrentBalance().add(request.getAmount()),
                    updatedAccount.getCurrentBalance());

        } catch (Exception e) {
            return createOrUpdateEntry(
                    utr,
                    request.getSenderAccountNumber(),
                    request.getAmount(),
                    TransactionType.DEBIT,
                    TransactionStatus.FAILED,
                    request.getIdempotencyKey(),
                    null,
                    null);
        }
    }

    @Transactional
    public Transaction processCredit(String utr, TransactionRequest request) {
        // 1️⃣ Claim idempotency FIRST
        Transaction tx = createInitiatedOrReturnExisting(
                utr,
                request.getReceiverAccountNumber(),
                request.getAmount(),
                TransactionType.CREDIT,
                request.getIdempotencyKey());

        if (tx.getStatus() != TransactionStatus.INITIATED) {
            return tx;
        }

        try {
            AccountDto updatedAccount = bankClient.credit(
                    BalanceModificationRequest.builder()
                            .accountNumber(request.getReceiverAccountNumber())
                            .amount(request.getAmount())
                            .build());

            return createOrUpdateEntry(
                    utr,
                    request.getReceiverAccountNumber(),
                    request.getAmount(),
                    TransactionType.CREDIT,
                    TransactionStatus.SUCCESS,
                    request.getIdempotencyKey(),
                    updatedAccount.getCurrentBalance().subtract(request.getAmount()),
                    updatedAccount.getCurrentBalance());

        } catch (Exception e) {
            return createOrUpdateEntry(
                    utr,
                    request.getReceiverAccountNumber(),
                    request.getAmount(),
                    TransactionType.CREDIT,
                    TransactionStatus.FAILED,
                    request.getIdempotencyKey(),
                    null,
                    null);
        }
    }

    private void compensateDebit(Transaction originalDebit, String originalIdempotencyKey) {
        String reversalKey = originalIdempotencyKey + "-REVERSAL";

        // Idempotency for Reversal
        Optional<Transaction> existingReversal = repository.findByIdempotencyKeyAndAccountNumberAndTransactionType(
                reversalKey,
                originalDebit.getAccountNumber(),
                TransactionType.CREDIT // Reversal of Debit is Credit
        );

        if (existingReversal.isPresent() && existingReversal.get().getStatus() == TransactionStatus.SUCCESS) {
            return; // Already reversed
        }


        try {
            // Perform Reversal (Credit back the sender)
            AccountDto restoredAccount = bankClient.credit(BalanceModificationRequest.builder()
                    .accountNumber(originalDebit.getAccountNumber())
                    .amount(originalDebit.getAmount())
                    .build());

            createOrUpdateEntry(
                    originalDebit.getUtr(),
                    originalDebit.getAccountNumber(),
                    originalDebit.getAmount(),
                    TransactionType.CREDIT,
                    TransactionStatus.SUCCESS,
                    reversalKey,
                    restoredAccount.getCurrentBalance().subtract(originalDebit.getAmount()),
                    restoredAccount.getCurrentBalance());

        } catch (Exception e) {
            // Critical Failure: Reversal Failed. Money is stuck!
            createOrUpdateEntry(
                    originalDebit.getUtr(),
                    originalDebit.getAccountNumber(),
                    originalDebit.getAmount(),
                    TransactionType.CREDIT,
                    TransactionStatus.FAILED,
                    reversalKey,
                    null, null);
            log.error("CRITICAL: Reversal failed for UTR {}, manual intervention required. Amount: {}, Account: {}",
                    originalDebit.getUtr(),
                    originalDebit.getAmount(),
                    originalDebit.getAccountNumber(), e);
        }
    }

    private Transaction createInitiatedOrReturnExisting(
            String utr,
            String account,
            BigDecimal amount,
            TransactionType type,
            String idempotencyKey) {
        try {
            Transaction tx = new Transaction();
            tx.setUtr(utr);
            tx.setAccountNumber(account);
            tx.setAmount(amount);
            tx.setTransactionType(type);
            tx.setStatus(TransactionStatus.INITIATED);
            tx.setIdempotencyKey(idempotencyKey);
            tx.setTimestamp(LocalDateTime.now());

            return repository.save(tx); // 🔐 atomic claim

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Already exists → safe read
            return repository.findByIdempotencyKeyAndAccountNumberAndTransactionType(
                    idempotencyKey, account, type).orElseThrow();
        }
    }

    private Transaction createOrUpdateEntry(String utr, String account, BigDecimal amount, TransactionType type,
            TransactionStatus status, String idempotencyKey,
            BigDecimal openingBalance, BigDecimal closingBalance) {

        // Check if exists to update, else create new
        Optional<Transaction> existing = repository.findByIdempotencyKeyAndAccountNumberAndTransactionType(
                idempotencyKey, account, type);

        Transaction tx = existing.orElse(new Transaction());
        tx.setUtr(utr);
        tx.setAccountNumber(account);
        tx.setAmount(amount);
        tx.setTransactionType(type);
        tx.setStatus(status);
        tx.setIdempotencyKey(idempotencyKey);
        tx.setOpeningBalance(openingBalance);
        tx.setClosingBalance(closingBalance);

        if (tx.getTimestamp() == null) {
            tx.setTimestamp(LocalDateTime.now());
        }

        return repository.save(tx);
    }

    // --- Deposit and Withdraw (Single Leg) ---

    public Transaction deposit(BalanceModificationRequest request, Long userId) {
        log.info("Deposit initiated: account={}, amount={}, key={}",
                request.getAccountNumber(),
                request.getAmount(),
                request.getIdempotencyKey());

        // Security Check: Ownership
        AccountDto account = bankClient.getAccount(request.getAccountNumber());
        if (!account.getUserId().equals(userId)) {
            log.error("Deposit authorization failed: account {} does not belong to user {}",
                    request.getAccountNumber(), userId);
            throw new RuntimeException("Unauthorized: Account does not belong to user");
        }

        // String key = UUID.randomUUID().toString(); // Internal Key
        String idempotencyKey = request.getIdempotencyKey();
        return processSingleLeg(request.getAccountNumber(), request.getAmount(), TransactionType.CREDIT,
                idempotencyKey);
    }

    public Transaction withdraw(BalanceModificationRequest request, Long userId) {
        log.info("Withdrawal initiated: account={}, amount={}, key={}",
                request.getAccountNumber(),
                request.getAmount(),
                request.getIdempotencyKey());

        // Security Check: Ownership
        AccountDto account = bankClient.getAccount(request.getAccountNumber());
        if (!account.getUserId().equals(userId)) {
            log.error("Withdrawal authorization failed: account {} does not belong to user {}",
                    request.getAccountNumber(), userId);
            throw new RuntimeException("Unauthorized: Account does not belong to user");
        }

        // String key = UUID.randomUUID().toString(); // Internal Key
        String idempotencyKey = request.getIdempotencyKey();
        return processSingleLeg(request.getAccountNumber(), request.getAmount(), TransactionType.DEBIT, idempotencyKey);
    }

    @Transactional
    public Transaction processSingleLeg(String accountNum, BigDecimal amount, TransactionType type, String key) {
        Optional<Transaction> existing = repository
                .findByIdempotencyKeyAndAccountNumberAndTransactionType(key, accountNum, type);

        String utr = existing.map(Transaction::getUtr)
                .orElse(UUID.randomUUID().toString());

        Transaction tx = createInitiatedOrReturnExisting(utr, accountNum, amount, type, key);

        if (tx.getStatus() != TransactionStatus.INITIATED) {
            return tx;
        }

        try {
            AccountDto result;
            BigDecimal opening, closing;

            if (type == TransactionType.DEBIT) {
                result = bankClient
                        .debit(BalanceModificationRequest.builder().accountNumber(accountNum).amount(amount).build());
                closing = result.getCurrentBalance();
                opening = closing.add(amount);
            } else {
                result = bankClient
                        .credit(BalanceModificationRequest.builder().accountNumber(accountNum).amount(amount).build());
                closing = result.getCurrentBalance();
                opening = closing.subtract(amount);
            }

            return createOrUpdateEntry(utr, accountNum, amount, type, TransactionStatus.SUCCESS, key, opening, closing);

        } catch (Exception e) {
            return createOrUpdateEntry(utr, accountNum, amount, type, TransactionStatus.FAILED, key, null, null);
        }
    }

    public java.util.List<Transaction> getAllTransactions() {
        return repository.findAll();
    }
}
