package com.fraud.service;

import com.fraud.dao.AlertDao;
import com.fraud.dao.TransactionDao;
import com.fraud.model.FraudAlert;
import com.fraud.model.Transaction;
import com.fraud.rules.Rule;
import com.fraud.rules.RuleResult;

import java.time.LocalDateTime;
import java.util.*;

public class DetectionService {
    private final TransactionDao txDao;
    private final AlertDao alertDao;
    private final List<Rule> rules;
    private final int highRiskThreshold;
    private final int mediumRiskThreshold;
    private final int velocityWindowSeconds;
    private final int velocityLimit;

    public DetectionService(TransactionDao txDao, AlertDao alertDao,
                            List<Rule> rules, int mediumRiskThreshold, int highRiskThreshold,
                            int velocityWindowSeconds, int velocityLimit) {

        this.txDao = txDao;
        this.alertDao = alertDao;
        this.rules = rules;
        this.mediumRiskThreshold = mediumRiskThreshold;
        this.highRiskThreshold = highRiskThreshold;
        this.velocityWindowSeconds = velocityWindowSeconds;
        this.velocityLimit = velocityLimit;
    }

    public Optional<FraudAlert> analyzeAndPersist(Transaction tx) {
        // 1) run stateless rules
        int totalScore = 0;
        List<String> reasons = new ArrayList<>();
        for (Rule rule : rules) {
            RuleResult result = rule.evaluate(tx);
            if (result != null && result.isMatched()) {
                totalScore += result.getScore();
                reasons.add(result.getRuleName() + ":" + result.getReason());
            }
        }

        // 2) velocity detection using txDao
        List<Transaction> recent = txDao.getRecentTransactions(tx.getAccountId(), velocityWindowSeconds);
        if (recent.size() >= velocityLimit) {
            int vWeight = 20; // choose some weight or make it configurable
            totalScore += vWeight;
            reasons.add("Velocity: " + recent.size() + " txns within last " + velocityWindowSeconds + "s");
        }

        // 3) duplicate detection: same amount + merchant in short time
        boolean duplicate = recent.stream().anyMatch(r ->
                r.getAmount() == tx.getAmount() && r.getMerchant().equalsIgnoreCase(tx.getMerchant()));
        if (duplicate) {
            int dWeight = 15;
            totalScore += dWeight;
            reasons.add("Duplicate: same amount+merchant in recent window");
        }

        // 4) determine risk level
        String risk;
        if (totalScore >= highRiskThreshold) risk = "HIGH";
        else if (totalScore >= mediumRiskThreshold) risk = "MEDIUM";
        else risk = "LOW";

        // 5) persist transaction & optional alert
        txDao.save(tx);
        if (!"LOW".equals(risk)) {
            String reasonTxt = String.join("; ", reasons);
            FraudAlert alert = new FraudAlert(tx.getTransactionId(), tx.getAccountId(), totalScore, risk, reasonTxt);
            alertDao.saveAlert(alert);
            return Optional.of(alert);
        }

        return Optional.empty();
    }
}
