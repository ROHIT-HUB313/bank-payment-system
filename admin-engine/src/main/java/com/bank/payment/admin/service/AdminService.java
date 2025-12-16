
package com.bank.payment.admin.service;

import com.bank.payment.admin.client.TransactionClient;
import com.bank.payment.admin.client.UserClient;
import com.bank.payment.admin.dto.TransactionDTO;
import com.bank.payment.admin.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserClient userClient;

    private final TransactionClient transactionClient;

    public List<UserDTO> getAllUsers(String role) {
        if (!"ADMIN".equals(role)) {
            throw new RuntimeException("ACCESS DENIED, ONLY ADMIN HAS PERMISSION");
        }
        return userClient.getAllUsers();
    }

    public List<TransactionDTO> getAllTransactions(String role) {
        if (!"ADMIN".equals(role)) {
            throw new RuntimeException("ACCESS DENIED, ONLY ADMIN HAS PERMISSION");
        }
        return transactionClient.getAllTransactions();
    }
}
