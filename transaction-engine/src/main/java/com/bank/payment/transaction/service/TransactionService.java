
package com.bank.payment.transaction.service;

import com.bank.payment.transaction.client.BankClient;
import com.bank.payment.transaction.dto.AccountDto;
import com.bank.payment.transaction.dto.BalanceModificationRequest;
import com.bank.payment.transaction.dto.TransactionRequest;
import com.bank.payment.transaction.entity.Transaction;
import com.bank.payment.transaction.entity.TransactionStatus;
import com.bank.payment.transaction.entity.TransactionType;
import com.bank.payment.transaction.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

// NOTE: This service uses synchronous Feign calls.
// Database transactions do NOT roll back remote bank-engine state.
@Service
@lombok.RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository repository;

    private final BankClient bankClient;

    /**
     * Executes a banking-correct transfer with strict idempotency, partial retry
     * support, and ledger integrity.
     */
    public Transaction transfer(TransactionRequest request, Long userId) {

        // 0. Security Check: Ownership
        AccountDto senderAccount = bankClient.getAccount(request.getSenderAccountNumber());
        if (!senderAccount.getUserId().equals(userId)) {
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
            compensateDebit(debitTx, request.getIdempotencyKey());
            // We return the primary Debit record, but its associated Credit failed and was
            // reversed.
            // Documentation: "On failure → mark CREDIT FAILED Also create a REVERSAL"
            throw new RuntimeException("Transfer Failed at Credit Stage. Amount Reversed.");
        }

        return debitTx;
    }

    private Transaction processDebit(String utr, TransactionRequest request) {
        // Create or Update INITIATED
        Transaction tx = createOrUpdateEntry(
                utr,
                request.getSenderAccountNumber(),
                request.getAmount(),
                TransactionType.DEBIT,
                TransactionStatus.INITIATED,
                request.getIdempotencyKey(),
                null, null // Null balances initially
        );

        try {
            // Call Bank Debit
            // Opening balance is derived from post-operation balance returned by
            // bank-engine.
            AccountDto updatedAccount = bankClient.debit(BalanceModificationRequest.builder()
                    .accountNumber(request.getSenderAccountNumber())
                    .amount(request.getAmount())
                    .build());

            // Success Updates
            return createOrUpdateEntry(
                    utr,
                    request.getSenderAccountNumber(),
                    request.getAmount(),
                    TransactionType.DEBIT,
                    TransactionStatus.SUCCESS,
                    request.getIdempotencyKey(),
                    updatedAccount.getCurrentBalance().add(request.getAmount()), // Opening
                    updatedAccount.getCurrentBalance() // Closing
            );

        } catch (Exception e) {
            // Fail Updates (Balances null)
            return createOrUpdateEntry(
                    utr,
                    request.getSenderAccountNumber(),
                    request.getAmount(),
                    TransactionType.DEBIT,
                    TransactionStatus.FAILED,
                    request.getIdempotencyKey(),
                    null, null);
        }
    }

    private Transaction processCredit(String utr, TransactionRequest request) {
        Transaction tx = createOrUpdateEntry(
                utr,
                request.getReceiverAccountNumber(),
                request.getAmount(),
                TransactionType.CREDIT,
                TransactionStatus.INITIATED,
                request.getIdempotencyKey(),
                null, null);

        try {
            // Call Bank Credit
            AccountDto updatedAccount = bankClient.credit(BalanceModificationRequest.builder()
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
                    updatedAccount.getCurrentBalance().subtract(request.getAmount()), // Opening
                    updatedAccount.getCurrentBalance() // Closing
            );
        } catch (Exception e) {
            return createOrUpdateEntry(
                    utr,
                    request.getReceiverAccountNumber(),
                    request.getAmount(),
                    TransactionType.CREDIT,
                    TransactionStatus.FAILED,
                    request.getIdempotencyKey(),
                    null, null);
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

        // Create Reversal Entry
        Transaction reversal = createOrUpdateEntry(
                originalDebit.getUtr(),
                originalDebit.getAccountNumber(),
                originalDebit.getAmount(),
                TransactionType.CREDIT,
                TransactionStatus.INITIATED,
                reversalKey,
                null, null);

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
            System.err.println("CRITICAL: Reversal Failed for UTR " + originalDebit.getUtr());
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
        // Security Check: Ownership
        AccountDto account = bankClient.getAccount(request.getAccountNumber());
        if (!account.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Account does not belong to user");
        }

        String key = UUID.randomUUID().toString(); // Internal Key
        return processSingleLeg(request.getAccountNumber(), request.getAmount(), TransactionType.CREDIT, key);
    }

    public Transaction withdraw(BalanceModificationRequest request, Long userId) {
        // Security Check: Ownership
        AccountDto account = bankClient.getAccount(request.getAccountNumber());
        if (!account.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Account does not belong to user");
        }

        String key = UUID.randomUUID().toString(); // Internal Key
        return processSingleLeg(request.getAccountNumber(), request.getAmount(), TransactionType.DEBIT, key);
    }

    private Transaction processSingleLeg(String accountNum, BigDecimal amount, TransactionType type, String key) {
        String utr = UUID.randomUUID().toString();

        Transaction tx = createOrUpdateEntry(utr, accountNum, amount, type, TransactionStatus.INITIATED, key, null,
                null);

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
