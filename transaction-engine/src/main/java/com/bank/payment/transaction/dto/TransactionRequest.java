
package com.bank.payment.transaction.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionRequest {

    private String senderAccountNumber;

    private String receiverAccountNumber;

    private BigDecimal amount;

    private String idempotencyKey;
}
