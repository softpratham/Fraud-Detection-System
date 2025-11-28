package com.fraud.app;

import com.fraud.config.ConfigLoader;
import com.fraud.dao.AlertDao;
import com.fraud.dao.DaoException;
import com.fraud.dao.TransactionDao;
import com.fraud.model.FraudAlert;
import com.fraud.model.Transaction;
import com.fraud.rules.Rule;
import com.fraud.rules.RuleResult;
import com.fraud.rules.impl.GeoLocationRule;
import com.fraud.rules.impl.HighAmountRule;
import com.fraud.service.DetectionService;
import com.fraud.service.ReportService;
import com.fraud.util.CsvReader;
import com.fraud.util.DBUtil;
import com.fraud.util.ShutdownUtil;
import com.zaxxer.hikari.HikariDataSource;
import com.fraud.rules.impl.NightTimeRule;
import com.fraud.rules.impl.ChannelRiskRule;
import com.fraud.rules.impl.RiskyMerchantRule;
import com.fasterxml.jackson.databind.JsonNode;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.*;

public class MainDay5 {

    public static void main(String[] args) throws Exception {

        ConfigLoader cfg = new ConfigLoader();
        Properties props = cfg.getProperties();
        DataSource ds = DBUtil.createDataSource(props);
        if (args != null && args.length > 0) {
            String cmd = args[0].trim().toLowerCase();
            try {
                switch (cmd) {
                    case "run-detection":
                        runDetection(cfg, ds);
                        break;
                    case "export-report":
                        String account = args.length > 1 ? args[1] : "acct123";
                        exportReport(ds, account, props.getProperty("report.output"));
                        break;
                    case "db-test":
                        dbTest(ds);
                        break;
                    default:
                        System.out.println("Unknown command: " + cmd);
                        printUsage();
                }
            } finally {
                ShutdownUtil.closeDataSource(ds);
            }
            return;
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            printMenu();
            String line = in.readLine();
            if (line == null) break;
            String choice = line.trim();

            try {
                switch (choice) {
                    case "1":
                        liveStreamStub();
                        break;

                    case "2": // Day 6 - Monthly Summary Reports / Analytics
                        monthlySummaryAnalytics(ds, in);
                        break;

                    case "3": // Search Alerts
                        searchAlerts(ds, in);
                        break;

                    case "4": // Search Transactions
                        searchTransactions(ds, in);
                        break;

                    case "5": // System Status
                        systemStatus(cfg, ds);
                        break;

                    case "6": // Export Reports (Day 4)
                        exportReportsInteractive(ds, props, in);
                        break;

                    case "7": // Configuration (Day 1 style)
                        showConfiguration(cfg);
                        break;

                    case "0":
                    case "q":
                    case "quit":
                    case "exit":
                        System.out.println("Shutting down.");
                        ShutdownUtil.closeDataSource(ds);
                        return;

                    default:
                        System.out.println("Unknown option: " + choice);
                }
            } catch (Exception ex) {
                System.err.println("Operation failed: " + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }

        ShutdownUtil.closeDataSource(ds);
    }

    // ==========================
    // Console UI helpers
    // ==========================
    private static void printMenu() {
        System.out.println();
        System.out.println("=========================================");
        System.out.println("     FRAUD ANALYST CONSOLE  v1.0");
        System.out.println("     Real-Time Monitoring System");
        System.out.println("=========================================");
        System.out.println();
        System.out.println("1) Live Stream Mode   (real-time detection)");
        System.out.println("2) Monthly Summary Reports  (Day 6 analytics)");
        System.out.println("3) Search Alerts");
        System.out.println("4) Search Transactions");
        System.out.println("5) System Status");
        System.out.println("6) Export Reports");
        System.out.println("7) Configuration");
        System.out.println("0) Exit");
        System.out.println("-----------------------------------------");
        System.out.print("Choose an option: ");
    }

    private static void printUsage() {
        System.out.println("Usage (non-interactive modes):");
        System.out.println("  mvn exec:java -Dexec.mainClass=com.fraud.app.MainDay5 -Dexec.args=\"run-detection\"");
        System.out.println("  mvn exec:java -Dexec.mainClass=com.fraud.app.MainDay5 -Dexec.args=\"export-report acct123\"");
        System.out.println("  mvn exec:java -Dexec.mainClass=com.fraud.app.MainDay5 -Dexec.args=\"db-test\"");
    }

    // ==========================
    // Option 1: Live Stream Mode (stub for now)
    // ==========================
    private static void liveStreamStub() {
        System.out.println("==== Live Stream Mode ====");
        System.out.println("Real-time transaction streaming is NOT enabled in this version.");
        System.out.println("Use CSV-based detection (via Monthly Reports / detection pipeline) instead.");
    }

    // ==========================
    // Option 2: Monthly Summary / Analytics (Day 6)
    // ==========================
    private static void monthlySummaryAnalytics(DataSource ds, BufferedReader in) throws Exception {
        System.out.println("==== Day 6: Monthly Summary / Analytics ====");
        System.out.print("Enter accountId for analytics (default acct123): ");
        String acc = in.readLine();
        if (acc == null || acc.trim().isEmpty()) acc = "acct123";

        AlertDao alertDao = new AlertDao(ds);
        List<FraudAlert> alerts = alertDao.getAlertsByAccount(acc, 1000);

        if (alerts.isEmpty()) {
            System.out.println("No alerts found for account: " + acc);
            return;
        }

        int total = alerts.size();
        int high = 0;
        int medium = 0;

        Map<String, Integer> reasonsCount = new HashMap<>();
        Map<String, Integer> monthBuckets = new TreeMap<>();
        LocalDateTime latest = null;

        for (FraudAlert a : alerts) {
            if ("HIGH".equalsIgnoreCase(a.getRiskLevel())) high++;
            if ("MEDIUM".equalsIgnoreCase(a.getRiskLevel())) medium++;

            String reason = a.getReason() != null ? a.getReason() : "Unknown";
            reasonsCount.put(reason, reasonsCount.getOrDefault(reason, 0) + 1);

            LocalDateTime ts = a.getCreatedAt();
            if (ts != null) {
                String ym = ts.getYear() + "-" + String.format("%02d", ts.getMonthValue());
                monthBuckets.put(ym, monthBuckets.getOrDefault(ym, 0) + 1);
                if (latest == null || ts.isAfter(latest)) {
                    latest = ts;
                }
            }
        }

        System.out.println("\n--- Alert Summary for account: " + acc + " ---");
        System.out.println("Total alerts: " + total);
        System.out.println("HIGH risk alerts: " + high);
        System.out.println("MEDIUM risk alerts: " + medium);

        System.out.println("\nAlerts per month:");
        for (Map.Entry<String, Integer> e : monthBuckets.entrySet()) {
            System.out.println("  " + e.getKey() + " -> " + e.getValue());
        }

        System.out.println("\nTop reasons:");
        reasonsCount.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(5)
                .forEach(e -> System.out.println("  " + e.getKey() + " -> " + e.getValue()));

        if (latest != null) {
            System.out.println("\nMost recent alert at: " + latest);
        }
        System.out.println("==== End of analytics ====");
    }

    // ==========================
    // Option 3: Search Alerts
    // ==========================
    private static void searchAlerts(DataSource ds, BufferedReader in) throws Exception {
        System.out.println("==== Search Alerts ====");
        System.out.print("Enter accountId (default acct123): ");
        String acc = in.readLine();
        if (acc == null || acc.trim().isEmpty()) acc = "acct123";

        AlertDao alertDao = new AlertDao(ds);
        List<FraudAlert> alerts = alertDao.getAlertsByAccount(acc, 100);

        if (alerts.isEmpty()) {
            System.out.println("No alerts found for account: " + acc);
            return;
        }

        System.out.println("Found " + alerts.size() + " alerts:");
        for (FraudAlert a : alerts) {
            System.out.println(a);
        }
    }

    // ==========================
    // Option 4: Search Transactions
    // ==========================
    private static void searchTransactions(DataSource ds, BufferedReader in) throws Exception {
        System.out.println("==== Search Transactions ====");
        System.out.print("Enter accountId (default acct123): ");
        String acc = in.readLine();
        if (acc == null || acc.trim().isEmpty()) acc = "acct123";

        System.out.print("Look-back window in seconds (default 3600): ");
        String w = in.readLine();
        int windowSeconds = 3600;
        try {
            if (w != null && !w.trim().isEmpty()) {
                windowSeconds = Integer.parseInt(w.trim());
            }
        } catch (NumberFormatException ignored) {}

        TransactionDao txDao = new TransactionDao(ds);
        List<Transaction> recent = txDao.getRecentTransactions(acc, windowSeconds);

        if (recent.isEmpty()) {
            System.out.println("No transactions found for account " + acc + " in last " + windowSeconds + " seconds.");
            return;
        }

        System.out.println("Found " + recent.size() + " transactions:");
        recent.forEach(System.out::println);
    }

    // ==========================
    // Option 5: System Status
    // ==========================
    private static void systemStatus(ConfigLoader cfg, DataSource ds) {
        System.out.println("==== System Status ====");
        Properties props = cfg.getProperties();

        System.out.println("DB URL : " + props.getProperty("db.url"));
        System.out.println("DB User: " + props.getProperty("db.user"));
        System.out.println("Thread pool size: " + props.getProperty("thread.pool.size"));

        System.out.println("\nFraud rules & weights:");
        cfg.getRuleWeights().forEach((k, v) -> System.out.println("  " + k + " = " + v));

        // DB connectivity check
        try {
            ds.getConnection().close();
            System.out.println("\nDatabase connection: OK");
        } catch (Exception e) {
            System.out.println("\nDatabase connection: FAILED -> " + e.getMessage());
        }

        if (ds instanceof HikariDataSource) {
            HikariDataSource hds = (HikariDataSource) ds;
            System.out.println("HikariCP pool: " + hds.getPoolName());
        }

        System.out.println("==== End of System Status ====");
    }

    // ==========================
    // Option 6: Export Reports
    // ==========================
    private static void exportReportsInteractive(DataSource ds, Properties props, BufferedReader in) throws Exception {
        System.out.println("==== Export Alerts Report (Day 4) ====");
        System.out.print("Enter accountId to export (default acct123): ");
        String account = in.readLine();
        if (account == null || account.trim().isEmpty()) account = "acct123";

        String outputFile = props.getProperty("report.output", "alerts_report.csv");
        exportReport(ds, account, outputFile);
    }

    private static void exportReport(DataSource ds, String accountId, String outputFile) {
        AlertDao alertDao = new AlertDao(ds);
        ReportService report = new ReportService(alertDao);
        report.exportAlerts(accountId, outputFile);
    }

    // ==========================
    // Option 7: Configuration (Day 1 style)
    // ==========================
    private static void showConfiguration(ConfigLoader cfg) {
        System.out.println("==== Configuration (Day 1) ====");
        Properties props = cfg.getProperties();
        System.out.println("application.properties:");
        props.forEach((k, v) -> System.out.println(k + " = " + v));

        System.out.println("\nFraud rule weights from rules.json:");
        cfg.getRuleWeights().forEach((k, v) -> System.out.println(k + " = " + v));

        System.out.println("==== End of Configuration ====");
    }

    // ==========================
    // Detection pipeline (used by Day 3 & non-interactive)
    // ==========================
    // ==========================
    // Detection pipeline (used by console + non-interactive)
    // ==========================
    private static void runDetection(ConfigLoader cfg, DataSource ds) throws Exception {
        Properties p = cfg.getProperties();
        TransactionDao txDao = new TransactionDao(ds);
        AlertDao alertDao = new AlertDao(ds);

        // ---- Build rules list from rules.json ----
        Map<String, Integer> ruleWeights = cfg.getRuleWeights();
        JsonNode root = cfg.getRulesNode();
        JsonNode rulesNode = root.get("rules");

        // 1) HighAmountRule
        double highAmountThreshold =
                Double.parseDouble(p.getProperty("high_amount_threshold", "50000"));
        int highAmountWeight = ruleWeights.getOrDefault("HighAmountRule", 30);
        List<Rule> rules = new ArrayList<>();
        rules.add(new HighAmountRule(highAmountThreshold, highAmountWeight));

        // 2) GeoLocationRule (riskyCountries from JSON)
        Set<String> riskyCountries = new HashSet<>();
        if (root.has("riskyCountries")) {
            root.get("riskyCountries").forEach(n -> riskyCountries.add(n.asText()));
        }
        int geoWeight = ruleWeights.getOrDefault("GeoLocationRule", 25);
        rules.add(new GeoLocationRule(riskyCountries, geoWeight));

        // 3) NightTimeRule (nightStartHour, nightEndHour from JSON)
        int nightStart = 0;
        int nightEnd = 5;
        int nightWeight = ruleWeights.getOrDefault("NightTimeRule", 20);
        if (rulesNode != null && rulesNode.has("NightTimeRule")) {
            JsonNode nNode = rulesNode.get("NightTimeRule");
            if (nNode.has("nightStartHour")) {
                nightStart = nNode.get("nightStartHour").asInt(0);
            }
            if (nNode.has("nightEndHour")) {
                nightEnd = nNode.get("nightEndHour").asInt(5);
            }
        }
        rules.add(new NightTimeRule(nightStart, nightEnd, nightWeight));

        // 4) ChannelRiskRule (weightOnline from JSON)
        int channelOnlineWeight = 15; // default
        if (rulesNode != null && rulesNode.has("ChannelRiskRule")) {
            JsonNode cNode = rulesNode.get("ChannelRiskRule");
            if (cNode.has("weightOnline")) {
                channelOnlineWeight = cNode.get("weightOnline").asInt(15);
            }
        }
        rules.add(new ChannelRiskRule(channelOnlineWeight));

        // 5) RiskyMerchantRule (riskyMerchants from JSON)
        Set<String> riskyMerchants = new HashSet<>();
        if (root.has("riskyMerchants")) {
            root.get("riskyMerchants").forEach(n -> riskyMerchants.add(n.asText()));
        }
        int riskyMerchantWeight = ruleWeights.getOrDefault("RiskyMerchantRule", 25);
        rules.add(new RiskyMerchantRule(riskyMerchants, riskyMerchantWeight));

        // ---- thresholds & velocity settings ----
        int medium = Integer.parseInt(p.getProperty("risk.score.medium", "30"));
        int high = Integer.parseInt(p.getProperty("risk.score.high", "60"));
        int velocitySeconds = Integer.parseInt(p.getProperty("velocity.window.seconds", "120"));
        int velocityLimit = Integer.parseInt(p.getProperty("velocity.limit", "3"));

        DetectionService svc =
                new DetectionService(txDao, alertDao, rules, medium, high,
                        velocitySeconds, velocityLimit);

        // ---- Load transactions from CSV & run detection ----
        List<Transaction> txns = CsvReader.readFromResource("/transactions.csv");
        System.out.println("Loaded " + txns.size() + " transactions. Running detection...");

        int alerts = 0;
        for (Transaction tx : txns) {
            try {
                Optional<FraudAlert> maybe = svc.analyzeAndPersist(tx);
                if (maybe.isPresent()) {
                    alerts++;
                    System.out.println("ALERT: " + maybe.get());
                }
            } catch (DaoException e) {
                // Gracefully ignore duplicate transaction IDs
                Throwable cause = e.getCause();
                if (cause instanceof SQLIntegrityConstraintViolationException) {
                    String msg = cause.getMessage();
                    if (msg != null && msg.contains("Duplicate entry")) {
                        System.out.println("Skipping duplicate transactionId=" + tx.getTransactionId());
                        continue;
                    }
                }
                throw e;
            }
        }
        System.out.println("Detection complete. Alerts created: " + alerts);
    }


    // ==========================
    // DB test helper
    // ==========================
    private static void dbTest(DataSource ds) {
        try {
            TransactionDao txDao = new TransactionDao(ds);
            Transaction tx = new Transaction(
                    "TEST_TXN_" + UUID.randomUUID().toString().substring(0, 8),
                    "acct123",
                    999.9,
                    "INR",
                    LocalDateTime.now(),
                    "TEST_MERCHANT",
                    "India",
                    "Card"
            );
            System.out.println("Saving test tx: " + tx.getTransactionId());
            txDao.save(tx);
            System.out.println("Fetching recent transactions for acct123:");
            List<Transaction> rec = txDao.getRecentTransactions("acct123", 3600);
            rec.forEach(System.out::println);
        } catch (Exception e) {
            throw new RuntimeException("DB test failed", e);
        }
    }
}
