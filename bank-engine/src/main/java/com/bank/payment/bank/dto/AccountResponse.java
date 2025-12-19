package com.bank.payment.bank.dto;

import com.bank.payment.bank.enums.AccountStatus;
import com.bank.payment.bank.enums.AccountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AccountResponse {
    private String accountNumber;
    private Long userId;
    private AccountType accountType;
    private AccountStatus accountStatus;
    private BigDecimal currentBalance;
    private String currency;
    private String ifscCode;
    private String address; // Branch Address
    private String createdBy;
    private LocalDateTime createdOn;
    private String updatedBy;
    private LocalDateTime updatedOn;
}
