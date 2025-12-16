package com.bank.payment.admin.client;

import com.bank.payment.admin.config.FeignConfig;
import com.bank.payment.admin.dto.TransactionDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@FeignClient(name = "transaction-engine", configuration = FeignConfig.class)
public interface TransactionClient {
    @GetMapping("/transactions/internal/all")
    List<TransactionDTO> getAllTransactions();
}
