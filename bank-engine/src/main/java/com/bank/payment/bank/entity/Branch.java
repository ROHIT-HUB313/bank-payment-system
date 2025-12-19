package com.bank.payment.bank.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bank_branch")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_code", unique = true, nullable = false)
    private String branchCode;

    @Column(name = "ifsc_code", nullable = false)
    private String ifscCode;

    @Column(name = "address", nullable = false)
    private String address;
}
