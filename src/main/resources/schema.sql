CREATE TABLE IF NOT EXISTS transactions (
    transaction_id   VARCHAR(50) PRIMARY KEY,
    account_id       VARCHAR(50) NOT NULL,
    amount           DOUBLE      NOT NULL,
    currency         VARCHAR(10) NOT NULL,
    txn_timestamp    DATETIME    NOT NULL,
    merchant         VARCHAR(100),
    location         VARCHAR(100),
    channel          VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS fraud_alerts (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(50) NOT NULL,
    account_id     VARCHAR(50) NOT NULL,
    score          INT NOT NULL,
    risk_level     VARCHAR(20) NOT NULL,
    reason         TEXT,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
