package com.fraud.dao;

import com.fraud.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionDao.class);

    private final DataSource ds;

    private static final String INSERT_SQL =
            "INSERT INTO transactions(transaction_id, account_id, amount, currency, txn_timestamp, merchant, location, channel) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_RECENT =
            "SELECT transaction_id, account_id, amount, currency, txn_timestamp, merchant, location, channel " +
                    "FROM transactions WHERE account_id = ? AND txn_timestamp >= ? ORDER BY txn_timestamp DESC";

    // helper to select since a timestamp (used by getTransactionsSince)
    private static final String SELECT_SINCE =
            "SELECT transaction_id, account_id, amount, currency, txn_timestamp, merchant, location, channel " +
                    "FROM transactions WHERE account_id = ? AND txn_timestamp >= ? ORDER BY txn_timestamp DESC";

    public TransactionDao(DataSource ds) {
        this.ds = ds;
    }

    /**
     * Persist transaction. Duplicate-key on transaction_id is treated as idempotent and logged as WARN.
     *
     * @param t transaction to save
     */
    public void save(Transaction t) {
        if (t == null) {
            LOGGER.warn("Attempted to save null transaction - ignoring.");
            return;
        }

        LOGGER.debug("Saving transaction id={} account={} amount={}",
                t.getTransactionId(), t.getAccountId(), t.getAmount());

        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT_SQL)) {

            ps.setString(1, t.getTransactionId());
            ps.setString(2, t.getAccountId());
            ps.setDouble(3, t.getAmount());
            ps.setString(4, t.getCurrency());
            ps.setTimestamp(5, Timestamp.valueOf(t.getTimestamp()));
            ps.setString(6, t.getMerchant());
            ps.setString(7, t.getLocation());
            ps.setString(8, t.getChannel());

            ps.executeUpdate();
            LOGGER.debug("Transaction saved successfully id={}", t.getTransactionId());

        } catch (SQLException e) {
            String sqlState = e.getSQLState();
            // SQLState class '23' denotes integrity constraint violation in SQL standard
            if (sqlState != null && sqlState.startsWith("23")) {
                LOGGER.warn("Duplicate transaction_id detected (SQLState={}): {} - ignoring duplicate insert.",
                        sqlState, t.getTransactionId());
                return;
            }
            LOGGER.error("Failed to save transaction {} (SQLState={}, errorCode={})",
                    t.getTransactionId(), e.getSQLState(), e.getErrorCode(), e);
            throw new DaoException("Failed to save transaction " + t.getTransactionId(), e);
        }
    }

    /**
     * Returns transactions for accountId since (now - windowSeconds).
     */
    public List<Transaction> getRecentTransactions(String accountId, int windowSeconds) {
        LocalDateTime since = LocalDateTime.now().minusSeconds(windowSeconds);
        return getTransactionsSince(accountId, since);
    }

    /**
     * Returns transactions for accountId since the provided LocalDateTime.
     */
    public List<Transaction> getTransactionsSince(String accountId, LocalDateTime since) {
        if (accountId == null || accountId.trim().isEmpty() || since == null) {
            LOGGER.debug("getTransactionsSince called with empty accountId or null since -> returning empty list.");
            return new ArrayList<>();
        }

        LOGGER.debug("Querying transactions for account={} since={}", accountId, since);

        List<Transaction> list = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_SINCE)) {

            ps.setString(1, accountId);
            ps.setTimestamp(2, Timestamp.valueOf(since));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String txId = rs.getString("transaction_id");
                    String acc = rs.getString("account_id");
                    double amount = rs.getDouble("amount");
                    String currency = rs.getString("currency");
                    Timestamp ts = rs.getTimestamp("txn_timestamp");
                    String merchant = rs.getString("merchant");
                    String location = rs.getString("location");
                    String channel = rs.getString("channel");

                    Transaction t = new Transaction(txId, acc, amount, currency, ts.toLocalDateTime(), merchant, location, channel);
                    list.add(t);
                }
            }

            LOGGER.debug("Fetched {} transaction(s) for account={}", list.size(), accountId);
            return list;

        } catch (SQLException e) {
            LOGGER.error("Failed to fetch transactions for account={} (SQLState={}, errorCode={})",
                    accountId, e.getSQLState(), e.getErrorCode(), e);
            throw new DaoException("Failed to fetch transactions for " + accountId, e);
        }
    }
}
