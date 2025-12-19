
package com.bank.payment.transaction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountDto {

    private String accountNumber;
    private BigDecimal currentBalance;
    private Long userId;
}
