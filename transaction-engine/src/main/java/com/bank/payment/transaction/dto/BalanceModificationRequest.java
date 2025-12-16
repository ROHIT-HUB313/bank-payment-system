
package com.bank.payment.transaction.dto;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;

@Data
@Builder
public class BalanceModificationRequest {

    private String accountNumber;
    private BigDecimal amount;
}
