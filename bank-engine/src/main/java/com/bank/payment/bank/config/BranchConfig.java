package com.bank.payment.bank.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class BranchConfig {

    private final Map<String, BranchDetails> branches = new HashMap<>();

    public BranchConfig() {

        branches.put("HEAD_OFFICE", new BranchDetails("BANK001", "Head Office, Mumbai, Maharashtra 400001"));
        branches.put("DELHI_OOFICE", new BranchDetails("BANK002", "Connaught Place Branch, New Delhi 110001"));
        branches.put("BANGALORE_OFFICE", new BranchDetails("BANK003", "Bangalore Main Branch, Karnataka 560001"));
        branches.put("CHENNAI_OFFICE", new BranchDetails("BANK004", "Chennai Central Branch, Tamil Nadu 600001"));

    }

    public Optional<BranchDetails> getBranchDetails(String branchCode) {
        return Optional.ofNullable(branches.get(branchCode));
    }

    public boolean isValidBranch(String branchCode) {
        return branches.containsKey(branchCode);
    }

    public String getDefaultBranchCode() {
        return "HEAD_OFFICE";
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BranchDetails {
        private String ifscCode;
        private String address;
    }
}
