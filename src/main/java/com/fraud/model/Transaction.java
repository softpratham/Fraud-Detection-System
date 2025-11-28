package com.fraud.model;

import java.time.LocalDateTime;

public class Transaction {
    private String transactionId;
    private String accountId;
    private double amount;
    private String currency;
    private LocalDateTime timestamp;
    private String merchant;
    private String location;
    private String channel;

    public Transaction() {}

    public Transaction(String transactionId, String accountId, double amount, String currency,
                       LocalDateTime timestamp, String merchant, String location, String channel) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.amount = amount;
        this.currency = currency;
        this.timestamp = timestamp;
        this.merchant = merchant;
        this.location = location;
        this.channel = channel;
    }

    // getters & setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId='" + transactionId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", timestamp=" + timestamp +
                ", merchant='" + merchant + '\'' +
                ", location='" + location + '\'' +
                ", channel='" + channel + '\'' +
                '}';
    }
}
