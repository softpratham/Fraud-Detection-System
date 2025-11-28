package com.fraud.rules.impl;

import com.fraud.model.Transaction;
import com.fraud.rules.Rule;
import com.fraud.rules.RuleResult;

/**
 * Triggers when transaction amount >= threshold.
 */
public class HighAmountRule implements Rule {
    private final double threshold;
    private final int weight;

    public HighAmountRule(double threshold, int weight) {
        this.threshold = threshold;
        this.weight = weight;
    }

    @Override
    public RuleResult evaluate(Transaction txn) {
        if (txn == null) return new RuleResult(name(), false, 0, "txn-null");
        if (txn.getAmount() >= threshold) {
            return new RuleResult(name(), true, weight, "HighAmount:" + txn.getAmount());
        }
        return new RuleResult(name(), false, 0, "ok");
    }

    @Override
    public String name() {
        return "HighAmountRule";
    }
}
