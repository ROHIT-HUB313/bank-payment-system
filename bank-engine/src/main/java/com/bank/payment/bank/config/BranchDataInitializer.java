package com.bank.payment.bank.config;

import com.bank.payment.bank.entity.Branch;
import com.bank.payment.bank.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BranchDataInitializer implements CommandLineRunner {

    private final BranchRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            log.info("Initializing Bank Branches...");
            List<Branch> branches = Arrays.asList(
                    new Branch(null, "HEAD_OFFICE", "HDFC0000001", "Head Office, Mumbai, MH 400001"),
                    new Branch(null, "DELHI_MAIN", "HDFC0000002", "Connaught Place, New Delhi, DL 110001"),
                    new Branch(null, "BANGALORE_MAIN", "HDFC0000003", "MG Road, Bangalore, KA 560001"),
                    new Branch(null, "CHENNAI_MAIN", "HDFC0000004", "Anna Salai, Chennai, TN 600002"));
            repository.saveAll(branches);
            log.info("{} branches initialized.", branches.size());
        }
    }
}
/*
Application starts
→ Spring creates beans (@Component)
→ Database & JPA ready
→ ApplicationContext ready
→ CommandLineRunner.run() executes ✅
→ App starts serving requests
*/