
package com.bank.payment.admin.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionDTO {
    private Long id;
    private String utr;
    private String accountNumber;
    private BigDecimal amount;
    private String transactionType;
    private String status;
    private String idempotencyKey;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private LocalDateTime timestamp;
}
