
package com.bank.payment.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionRequest {

    @NotBlank(message = "Sender account number is required")
    private String senderAccountNumber;

    @NotBlank(message = "Receiver account number is required")
    private String receiverAccountNumber;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;
}
