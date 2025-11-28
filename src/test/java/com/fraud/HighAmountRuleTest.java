package com.fraud;

import com.fraud.model.Transaction;
import com.fraud.rules.impl.HighAmountRule;
import com.fraud.rules.RuleResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class HighAmountRuleTest {

    @Test
    public void testHighAmountTriggers() {
        double threshold = 50000.0;
        int weight = 30;
        HighAmountRule rule = new HighAmountRule(threshold, weight);

        Transaction tHigh = new Transaction("T1", "acct1", 75000.0, "INR",
                LocalDateTime.now(), "M", "India", "Card");

        RuleResult res = rule.evaluate(tHigh);
        assertTrue(res.isMatched());
        assertEquals(weight, res.getScore());
    }

    @Test
    public void testBelowThresholdDoesNotTrigger() {
        double threshold = 50000.0;
        int weight = 30;
        HighAmountRule rule = new HighAmountRule(threshold, weight);

        Transaction tLow = new Transaction("T2", "acct1", 100.0, "INR",
                LocalDateTime.now(), "M", "India", "Card");

        RuleResult res = rule.evaluate(tLow);
        assertFalse(res.isMatched());
        assertEquals(0, res.getScore());
    }
}
