package com.fraud.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fraud.config.ConfigLoader;
import com.fraud.rules.Rule;
import com.fraud.rules.impl.*;

import java.util.*;

public class RuleFactory {

    public static List<Rule> createRules(ConfigLoader cfg) {
        List<Rule> rules = new ArrayList<>();
        Properties props = cfg.getProperties();
        JsonNode root = cfg.getRulesNode();
        JsonNode rulesNode = root.get("rules");

        if (rulesNode == null) return rules;

        // 1. High Amount Rule
        if (rulesNode.has("HighAmountRule")) {
            JsonNode n = rulesNode.get("HighAmountRule");
            if (n.path("enabled").asBoolean(true)) {
                // specific threshold from properties, weight from JSON
                double threshold = Double.parseDouble(props.getProperty("high_amount_threshold", "50000"));
                int weight = n.path("weight").asInt(30);
                rules.add(new HighAmountRule(threshold, weight));
            }
        }

        // 2. Geo Location Rule
        if (rulesNode.has("GeoLocationRule")) {
            JsonNode n = rulesNode.get("GeoLocationRule");
            if (n.path("enabled").asBoolean(true)) {
                int weight = n.path("weight").asInt(25);
                Set<String> riskyCountries = new HashSet<>();
                if (root.has("riskyCountries")) {
                    root.get("riskyCountries").forEach(c -> riskyCountries.add(c.asText()));
                }
                rules.add(new GeoLocationRule(riskyCountries, weight));
            }
        }

        // 3. Night Time Rule
        if (rulesNode.has("NightTimeRule")) {
            JsonNode n = rulesNode.get("NightTimeRule");
            if (n.path("enabled").asBoolean(true)) {
                int weight = n.path("weight").asInt(20);
                int start = n.path("nightStartHour").asInt(0);
                int end = n.path("nightEndHour").asInt(5);
                rules.add(new NightTimeRule(start, end, weight));
            }
        }

        // 4. Channel Risk Rule
        if (rulesNode.has("ChannelRiskRule")) {
            JsonNode n = rulesNode.get("ChannelRiskRule");
            if (n.path("enabled").asBoolean(true)) {
                int weight = n.path("weightOnline").asInt(15);
                rules.add(new ChannelRiskRule(weight));
            }
        }

        // 5. Risky Merchant Rule
        if (rulesNode.has("RiskyMerchantRule")) {
            JsonNode n = rulesNode.get("RiskyMerchantRule");
            if (n.path("enabled").asBoolean(true)) {
                int weight = n.path("weight").asInt(25);
                Set<String> riskyMerchants = new HashSet<>();
                if (root.has("riskyMerchants")) {
                    root.get("riskyMerchants").forEach(m -> riskyMerchants.add(m.asText()));
                }
                rules.add(new RiskyMerchantRule(riskyMerchants, weight));
            }
        }

        return rules;
    }
}