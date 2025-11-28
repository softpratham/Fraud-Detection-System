package com.fraud.rules.impl;

import com.fraud.model.Transaction;
import com.fraud.rules.Rule;
import com.fraud.rules.RuleResult;

import java.util.Set;

/**
 * Triggers when transaction location is in a configured risky countries set.
 */
public class GeoLocationRule implements Rule {
    private final Set<String> riskyCountries;
    private final int weight;

    public GeoLocationRule(Set<String> riskyCountries, int weight) {
        this.riskyCountries = riskyCountries;
        this.weight = weight;
    }

    @Override
    public RuleResult evaluate(Transaction txn) {
        if (txn == null) return new RuleResult(name(), false, 0, "txn-null");
        String loc = txn.getLocation();
        if (loc == null || loc.trim().isEmpty()) return new RuleResult(name(), false, 0, "no-location");
        String norm = loc.trim();
        for (String r : riskyCountries) {
            if (r != null && r.equalsIgnoreCase(norm)) {
                return new RuleResult(name(), true, weight, "RiskCountry:" + norm);
            }
        }
        return new RuleResult(name(), false, 0, "ok");
    }

    @Override
    public String name() {
        return "GeoLocationRule";
    }
}
