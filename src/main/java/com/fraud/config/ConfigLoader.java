package com.fraud.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

public class ConfigLoader {
    private static final String PROPS = "/application.properties";
    private static final String RULES = "/rules.json";

    private final Properties properties = new Properties();
    private final JsonNode rulesNode;

    public ConfigLoader() throws IOException {
        // load properties
        try (InputStream is = getClass().getResourceAsStream(PROPS)) {
            if (is == null) throw new IOException("application.properties not found in resources");
            properties.load(is);
        }

        // load rules.json
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = getClass().getResourceAsStream(RULES)) {
            if (is == null) throw new IOException("rules.json not found in resources");
            rulesNode = mapper.readTree(is);
        }
    }

    public Properties getProperties() {
        return properties;
    }

    public JsonNode getRulesNode() {
        return rulesNode;
    }

    /**
     * Convenience method to extract rule weights from rules.json into a Map.
     * Expected structure in rules.json:
     * {
     *   "rules": {
     *     "HighAmountRule": {"enabled": true, "weight": 30},
     *     ...
     *   }
     * }
     */
    public Map<String, Integer> getRuleWeights() {
        Map<String, Integer> map = new HashMap<>();
        JsonNode rules = rulesNode.get("rules");
        if (rules != null && rules.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = rules.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> e = fields.next();
                JsonNode weightNode = e.getValue().get("weight");
                if (weightNode != null && weightNode.isNumber()) {
                    map.put(e.getKey(), weightNode.asInt());
                }
            }
        }
        return map;
    }
}