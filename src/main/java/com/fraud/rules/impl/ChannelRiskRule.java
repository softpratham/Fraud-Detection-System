package com.fraud.rules.impl;

import com.fraud.model.Transaction;
import com.fraud.rules.Rule;
import com.fraud.rules.RuleResult;

public class ChannelRiskRule implements Rule {

    private final int onlineWeight;

    public ChannelRiskRule(int onlineWeight) {
        this.onlineWeight = onlineWeight;
    }

    @Override
    public RuleResult evaluate(Transaction txn) {
        if (txn == null) {
            return new RuleResult(name(), false, 0, "txn-null");
        }
        String ch = txn.getChannel();
        if (ch == null) {
            return new RuleResult(name(), false, 0, "no-channel");
        }
        String norm = ch.trim().toLowerCase();

        if (norm.equals("online")) {
            return new RuleResult(name(), true, onlineWeight, "Channel:ONLINE");
        }
        // You can extend: card-not-present, international-online, etc.
        return new RuleResult(name(), false, 0, "ok");
    }

    @Override
    public String name() {
        return "ChannelRiskRule";
    }
}
