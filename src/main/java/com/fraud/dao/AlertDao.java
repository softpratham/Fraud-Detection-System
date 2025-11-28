package com.fraud.dao;

import com.fraud.model.FraudAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AlertDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertDao.class);

    private final DataSource ds;

    private static final String INSERT_ALERT =
            "INSERT INTO fraud_alerts(transaction_id, account_id, score, risk_level, reason) VALUES (?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ACCOUNT =
            "SELECT id, transaction_id, account_id, score, risk_level, reason, created_at FROM fraud_alerts WHERE account_id = ? ORDER BY created_at DESC LIMIT ?";

    public AlertDao(DataSource ds) {
        this.ds = ds;
    }

    /**
     * Persist a FraudAlert. Logs success or failure (and assigns generated id on success).
     *
     * @param a alert to save
     */
    public void saveAlert(FraudAlert a) {
        if (a == null) {
            LOGGER.warn("saveAlert called with null alert - ignoring.");
            return;
        }

        LOGGER.debug("Saving alert for tx={} account={} score={} risk={}",
                a.getTransactionId(), a.getAccountId(), a.getScore(), a.getRiskLevel());

        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT_ALERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, a.getTransactionId());
            ps.setString(2, a.getAccountId());
            ps.setInt(3, a.getScore());
            ps.setString(4, a.getRiskLevel());
            ps.setString(5, a.getReason());

            int updated = ps.executeUpdate();
            if (updated == 0) {
                LOGGER.warn("No rows inserted for alert tx={}", a.getTransactionId());
            } else {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        long generatedId = rs.getLong(1);
                        a.setId(generatedId);
                        LOGGER.debug("Alert saved with id={} for tx={}", generatedId, a.getTransactionId());
                    } else {
                        LOGGER.debug("Alert saved but no generated key returned for tx={}", a.getTransactionId());
                    }
                }
            }

        } catch (SQLException e) {
            LOGGER.error("Failed to save alert for tx={} (SQLState={}, errorCode={})",
                    a.getTransactionId(), e.getSQLState(), e.getErrorCode(), e);
            throw new DaoException("Failed to save alert for tx " + a.getTransactionId(), e);
        }
    }

    /**
     * Return most recent alerts for the given account (ordered by created_at desc).
     *
     * @param accountId account id
     * @param limit max rows
     * @return list of FraudAlert
     */
    public List<FraudAlert> getAlertsByAccount(String accountId, int limit) {
        if (accountId == null || accountId.trim().isEmpty()) {
            LOGGER.debug("getAlertsByAccount called with empty accountId -> returning empty list.");
            return new ArrayList<>();
        }
        if (limit <= 0) limit = 50;

        LOGGER.debug("Querying up to {} alerts for account={}", limit, accountId);

        List<FraudAlert> list = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_ACCOUNT)) {

            ps.setString(1, accountId);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FraudAlert a = new FraudAlert();
                    a.setId(rs.getLong("id"));
                    a.setTransactionId(rs.getString("transaction_id"));
                    a.setAccountId(rs.getString("account_id"));
                    a.setScore(rs.getInt("score"));
                    a.setRiskLevel(rs.getString("risk_level"));
                    a.setReason(rs.getString("reason"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) a.setCreatedAt(ts.toLocalDateTime());
                    list.add(a);
                }
            }

            LOGGER.debug("Fetched {} alert(s) for account={}", list.size(), accountId);
            return list;

        } catch (SQLException e) {
            LOGGER.error("Failed to query alerts for account={} (SQLState={}, errorCode={})",
                    accountId, e.getSQLState(), e.getErrorCode(), e);
            throw new DaoException("Failed to query alerts for " + accountId, e);
        }
    }
}
