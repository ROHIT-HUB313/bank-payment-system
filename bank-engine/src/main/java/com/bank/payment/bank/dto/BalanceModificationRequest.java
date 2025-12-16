
package com.bank.payment.bank.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BalanceModificationRequest {

    private String accountNumber;
    private BigDecimal amount;
}
