package com.fraud.model;

import java.time.LocalDateTime;

public class FraudAlert {
    private long id;
    private String transactionId;
    private String accountId;
    private int score;
    private String riskLevel;
    private String reason;
    private LocalDateTime createdAt;

    public FraudAlert() {}

    public FraudAlert(String transactionId, String accountId, int score, String riskLevel, String reason) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.score = score;
        this.riskLevel = riskLevel;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }

    // getters & setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "FraudAlert{" +
                "id=" + id +
                ", transactionId='" + transactionId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", score=" + score +
                ", riskLevel='" + riskLevel + '\'' +
                ", reason='" + reason + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}