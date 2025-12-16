
package com.bank.payment.transaction.repository;

import com.bank.payment.transaction.entity.Transaction;
import com.bank.payment.transaction.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdempotencyKeyAndAccountNumberAndTransactionType(
            String idempotencyKey,
            String accountNumber,
            TransactionType transactionType);

    List<Transaction> findByAccountNumber(String accountNumber);

    List<Transaction> findByUtr(String utr);
}
