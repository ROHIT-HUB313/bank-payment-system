
package com.bank.payment.bank.repository;

import com.bank.payment.bank.entity.Account;
import com.bank.payment.bank.enums.AccountStatus;
import com.bank.payment.bank.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUserId(Long userId);

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByAccountTypeAndAccountStatus(AccountType type,
                                                    AccountStatus status);
}
