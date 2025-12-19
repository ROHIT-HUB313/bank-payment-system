package com.bank.payment.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateAccountRequest {

    private static final String ALLOWED_TYPES_MSG = "Must be one of: SAVINGS, CURRENT, BUSINESS, SALARY";
    private static final String ALLOWED_CURRENCY_MSG = "Currently only INR is supported";

    @NotBlank(message = "Account Type is required")
    private String accountType;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^(INR)$", message = ALLOWED_CURRENCY_MSG)
    private String currency;

    private String branchCode; // Optional, defaults to HEAD_OFFICE
}
