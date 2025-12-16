
package com.bank.payment.transaction.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AccountDto {

    private String accountNumber;
    private BigDecimal currentBalance;
    private Long userId;
}
