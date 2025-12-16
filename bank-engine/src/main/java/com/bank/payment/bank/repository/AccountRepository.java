
package com.bank.payment.bank.repository;

import com.bank.payment.bank.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUserId(Long userId);

    Optional<Account> findByAccountNumber(String accountNumber);
}
