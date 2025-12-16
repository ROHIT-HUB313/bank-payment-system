
package com.bank.payment.transaction.client;

import com.bank.payment.transaction.dto.AccountDto;
import com.bank.payment.transaction.dto.BalanceModificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.bank.payment.transaction.config.FeignConfig;

@FeignClient(name = "bank-engine", configuration = FeignConfig.class)
public interface BankClient {
    @GetMapping("/accounts/public/user")
    AccountDto getAccount(@RequestParam("accountNo") String accountNo);

    @PostMapping("/accounts/internal/credit")
    AccountDto credit(@RequestBody BalanceModificationRequest request);

    @PostMapping("/accounts/internal/debit")
    AccountDto debit(@RequestBody BalanceModificationRequest request);
}
