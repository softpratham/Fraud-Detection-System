package com.fraud.rules.impl;

import com.fraud.model.Transaction;
import com.fraud.rules.Rule;
import com.fraud.rules.RuleResult;

import java.util.HashSet;
import java.util.Set;

public class RiskyMerchantRule implements Rule {

    private final Set<String> riskyMerchants; // Stored as UPPERCASE
    private final int weight;

    public RiskyMerchantRule(Set<String> rawMerchants, int weight) {
        this.weight = weight;
        // Optimization: Pre-process set to Uppercase for O(1) lookup
        this.riskyMerchants = new HashSet<>();
        if (rawMerchants != null) {
            for (String m : rawMerchants) {
                if (m != null) this.riskyMerchants.add(m.trim().toUpperCase());
            }
        }
    }

    @Override
    public RuleResult evaluate(Transaction txn) {
        if (txn == null) return new RuleResult(name(), false, 0, "txn-null");

        String merchant = txn.getMerchant();
        if (merchant == null) return new RuleResult(name(), false, 0, "no-merchant");

        // Fast lookup
        if (riskyMerchants.contains(merchant.trim().toUpperCase())) {
            return new RuleResult(name(), true, weight, "RiskyMerchant:" + merchant);
        }

        return new RuleResult(name(), false, 0, "ok");
    }

    @Override
    public String name() {
        return "RiskyMerchantRule";
    }
}