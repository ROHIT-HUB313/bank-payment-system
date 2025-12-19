package com.bank.payment.bank.mapper;

import com.bank.payment.bank.dto.AccountResponse;
import com.bank.payment.bank.entity.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountResponse toResponse(Account account) {
        if (account == null) {
            return null;
        }
        return AccountResponse.builder()
                .accountNumber(account.getAccountNumber())
                .userId(account.getUserId())
                .accountType(account.getAccountType())
                .accountStatus(account.getAccountStatus())
                .currentBalance(account.getCurrentBalance())
                .currency(account.getCurrency())
                .ifscCode(account.getIfscCode())
                .address(account.getAddress())
                .createdBy(account.getCreatedBy())
                .createdOn(account.getCreatedOn())
                .updatedBy(account.getUpdatedBy())
                .updatedOn(account.getUpdatedOn())
                .build();
    }
}
