package com.fraud.rules.impl;

import com.fraud.model.Transaction;
import com.fraud.rules.Rule;
import com.fraud.rules.RuleResult;

import java.time.LocalDateTime;

/**
 * Triggers if transaction time is between [nightStartHour, nightEndHour).
 */
public class NightTimeRule implements Rule {

    private final int nightStartHour; // inclusive
    private final int nightEndHour;   // exclusive
    private final int weight;

    public NightTimeRule(int nightStartHour, int nightEndHour, int weight) {
        this.nightStartHour = nightStartHour;
        this.nightEndHour = nightEndHour;
        this.weight = weight;
    }

    @Override
    public RuleResult evaluate(Transaction txn) {
        if (txn == null || txn.getTimestamp() == null) {
            return new RuleResult(name(), false, 0, "no-timestamp");
        }

        LocalDateTime ts = txn.getTimestamp();
        int hour = ts.getHour();
        boolean inNightWindow;

        if (nightStartHour <= nightEndHour) {
            // Simple window (e.g. 0-5)
            inNightWindow = hour >= nightStartHour && hour < nightEndHour;
        } else {
            // Wrap around midnight (e.g. 22-3)
            inNightWindow = (hour >= nightStartHour || hour < nightEndHour);
        }

        if (inNightWindow) {
            return new RuleResult(name(), true, weight, "NightTimeHour:" + hour);
        }
        return new RuleResult(name(), false, 0, "ok");
    }

    @Override
    public String name() {
        return "NightTimeRule";
    }
}
