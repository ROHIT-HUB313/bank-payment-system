package com.bank.payment.admin.service;

import com.bank.payment.admin.client.TransactionClient;
import com.bank.payment.admin.client.UserClient;
import com.bank.payment.admin.dto.TransactionDTO;
import com.bank.payment.admin.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserClient userClient;

    private final TransactionClient transactionClient;

    public List<UserDTO> getAllUsers(String role) {
        log.info("Admin action: Fetching all users, requestedBy role={}", role);
        if (!"ADMIN".equals(role)) {
            log.error("Unauthorized admin access attempt: role={}", role);
            throw new RuntimeException("Unauthorized: Admin access required");
        }
        List<UserDTO> users = userClient.getAllUsers();
        log.info("Admin action: Retrieved {} users", users.size());
        return users;
    }

    public List<TransactionDTO> getAllTransactions(String role) {
        log.info("Admin action: Fetching all transactions, requestedBy role={}", role);
        if (!"ADMIN".equals(role)) {
            log.error("Unauthorized admin access attempt: role={}", role);
            throw new RuntimeException("Unauthorized: Admin access required");
        }
        List<TransactionDTO> transactions = transactionClient.getAllTransactions();
        log.info("Admin action: Retrieved {} transactions", transactions.size());
        return transactions;
    }
}
