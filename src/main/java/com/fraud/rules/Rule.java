package com.fraud.rules;

import com.fraud.model.Transaction;

/**
 * Simple rule: evaluates single transaction and returns a RuleResult.
 */
public interface Rule {
    /**
     * Evaluate this rule for a single transaction.
     *
     * @param txn the transaction under inspection
     * @return RuleResult (contains matched flag and score contribution)
     */
    RuleResult evaluate(Transaction txn);
    default String name() {
        // fallback name - implementations can override
        return this.getClass().getSimpleName();
    }
}
