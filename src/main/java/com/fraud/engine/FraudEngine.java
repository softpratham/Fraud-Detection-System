package com.fraud.engine;

import com.fraud.model.Transaction;
import com.fraud.rules.Rule;
import com.fraud.rules.RuleResult;

import java.util.List;

/**
 * Small helper engine that evaluates a list of rules for a transaction.
 */
public class FraudEngine {

    private final List<Rule> rules;

    public FraudEngine(List<Rule> rules) {
        this.rules = rules;
    }

    public int evaluate(Transaction t) {
        int score = 0;
        for (Rule r : rules) {
            RuleResult rr = r.evaluate(t);
            if (rr != null && rr.isMatched()) score += rr.getScore();
        }
        return score;
    }
}
