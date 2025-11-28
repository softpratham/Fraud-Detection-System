package com.fraud.app;

import com.fraud.config.ConfigLoader;
import com.fraud.dao.AlertDao;
import com.fraud.dao.TransactionDao;
import com.fraud.engine.RuleFactory;
import com.fraud.model.FraudAlert;
import com.fraud.model.Transaction;
import com.fraud.rules.Rule;
import com.fraud.service.DetectionService;
import com.fraud.service.ReportService;
import com.fraud.util.CsvReader;
import com.fraud.util.DBUtil;
import com.fraud.util.ShutdownUtil;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        log.info("Starting Fraud Detection System");

        ConfigLoader cfg = new ConfigLoader();
        Properties props = cfg.getProperties();
        DataSource ds = DBUtil.createDataSource(props);

        // --------------------------------------------------------------------
        // NON-INTERACTIVE COMMAND MODE
        // --------------------------------------------------------------------
        if (args != null && args.length > 0) {
            String cmd = args[0].trim().toLowerCase();

            try {
                switch (cmd) {
                    case "run-detection":
                        runDetection(cfg, ds);
                        break;

                    case "export-report":
                        exportReport(
                                ds,
                                args.length > 1 ? args[1] : "acct123",
                                props.getProperty("report.output", "alerts_report.csv")
                        );
                        break;

                    case "export-pdf":
                        exportReportPdf(
                                ds,
                                args.length > 1 ? args[1] : "acct123",
                                args.length > 2 ? args[2] : "alerts_report.pdf"
                        );
                        break;

                    case "export-json":
                        exportReportJson(
                                ds,
                                args.length > 1 ? args[1] : "acct123",
                                args.length > 2 ? args[2] : "alerts_report.json"
                        );
                        break;

                    case "db-test":
                        dbTest(ds);
                        break;

                    default:
                        log.warn("Unknown command: {}", cmd);
                        printUsage();
                }

            } finally {
                ShutdownUtil.closeDataSource(ds);
            }
            return;
        }

        // --------------------------------------------------------------------
        // INTERACTIVE CONSOLE MODE
        // --------------------------------------------------------------------
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                printMenu();
                String choice = in.readLine();

                if (choice == null) break;
                choice = choice.trim();

                try {
                    switch (choice) {
                        case "1": liveStreamStub(); break;
                        case "2": monthlySummaryAnalytics(ds, in); break;
                        case "3": searchAlerts(ds, in); break;
                        case "4": searchTransactions(ds, in); break;
                        case "5": systemStatus(cfg, ds); break;
                        case "6": exportCsvInteractive(ds, props, in); break;
                        case "7": showConfiguration(cfg); break;
                        case "8": runDetection(cfg, ds); break;
                        case "9": exportPdfInteractive(ds, props, in); break;
                        case "10": exportJsonInteractive(ds, props, in); break;

                        case "0":
                        case "exit":
                        case "quit":
                            log.info("Shutting down...");
                            ShutdownUtil.closeDataSource(ds);
                            return;

                        default:
                            System.out.println("Unknown option.");
                    }
                } catch (Exception ex) {
                    log.error("Operation failed", ex);
                    System.out.println("Error: " + ex.getMessage());
                }
            }
        }

        ShutdownUtil.closeDataSource(ds);
    }

    // --------------------------------------------------------------------
    // MENU
    // --------------------------------------------------------------------
    private static void printMenu() {
        System.out.println();
        System.out.println("=========================================");
        System.out.println("        FRAUD ANALYST CONSOLE v1.0       ");
        System.out.println("=========================================");
        System.out.println("1) Live Stream Mode (stub)");
        System.out.println("2) Monthly Summary Reports");
        System.out.println("3) Search Alerts");
        System.out.println("4) Search Transactions");
        System.out.println("5) System Status");
        System.out.println("6) Export Alerts (CSV)");
        System.out.println("7) Show Configuration");
        System.out.println("8) Run Detection Pipeline");
        System.out.println("9) Export Alerts (PDF)");
        System.out.println("10) Export Alerts (JSON)");
        System.out.println("0) Exit");
        System.out.println("-----------------------------------------");
        System.out.print("Choose an option: ");
    }

    // --------------------------------------------------------------------
    // MENU: Live Stream Stub
    // --------------------------------------------------------------------
    private static void liveStreamStub() {
        log.info("Live stream mode not implemented.");
        System.out.println("Real-time streaming disabled in this version.");
    }

    // --------------------------------------------------------------------
    // MENU: Monthly Summary Analytics
    // --------------------------------------------------------------------
    private static void monthlySummaryAnalytics(DataSource ds, BufferedReader in) throws Exception {
        System.out.println("Enter accountId (default acct123): ");
        String acc = in.readLine();
        if (acc == null || acc.trim().isEmpty()) acc = "acct123";

        AlertDao dao = new AlertDao(ds);
        List<FraudAlert> list = dao.getAlertsByAccount(acc, 1000);

        if (list.isEmpty()) {
            System.out.println("No alerts found.");
            return;
        }

        long high = list.stream().filter(a -> "HIGH".equalsIgnoreCase(a.getRiskLevel())).count();
        long med  = list.stream().filter(a -> "MEDIUM".equalsIgnoreCase(a.getRiskLevel())).count();

        System.out.println("Total alerts: " + list.size());
        System.out.println("HIGH risk:   " + high);
        System.out.println("MEDIUM risk: " + med);
    }

    // --------------------------------------------------------------------
    // MENU: Search Alerts
    // --------------------------------------------------------------------
    private static void searchAlerts(DataSource ds, BufferedReader in) throws Exception {
        System.out.print("Enter accountId: ");
        String acc = in.readLine();
        if (acc == null || acc.trim().isEmpty()) acc = "acct123";
        new AlertDao(ds).getAlertsByAccount(acc, 100).forEach(System.out::println);
    }

    // --------------------------------------------------------------------
    // MENU: Search Transactions
    // --------------------------------------------------------------------
    private static void searchTransactions(DataSource ds, BufferedReader in) throws Exception {
        System.out.print("Enter accountId: ");
        String acc = in.readLine();
        if (acc == null || acc.trim().isEmpty()) acc = "acct123";

        TransactionDao dao = new TransactionDao(ds);
        List<Transaction> list = dao.getRecentTransactions(acc, 3600);

        if (list.isEmpty()) System.out.println("No transactions found.");
        list.forEach(System.out::println);
    }

    // --------------------------------------------------------------------
    // MENU: System Status
    // --------------------------------------------------------------------
    private static void systemStatus(ConfigLoader cfg, DataSource ds) {
        System.out.println("DB URL: " + cfg.getProperties().getProperty("db.url"));
        try {
            ds.getConnection().close();
            System.out.println("DB OK");
        } catch (Exception e) {
            System.out.println("DB FAILED: " + e.getMessage());
        }
    }

    // --------------------------------------------------------------------
    // MENU: Export CSV
    // --------------------------------------------------------------------
    private static void exportCsvInteractive(DataSource ds, Properties props, BufferedReader in)
            throws Exception {
        System.out.print("Enter accountId: ");
        String acc = in.readLine();
        if (acc == null || acc.trim().isEmpty()) acc = "acct123";

        String file = props.getProperty("report.output", "alerts_report.csv");
        exportReport(ds, acc, file);
        System.out.println("CSV saved: " + file);
    }

    // EXPORT CSV BASE FUNCTION
    private static void exportReport(DataSource ds, String acc, String file) {
        new ReportService(new AlertDao(ds)).exportAlerts(acc, file);
    }

    // --------------------------------------------------------------------
    // MENU: Export PDF
    // --------------------------------------------------------------------
    private static void exportPdfInteractive(DataSource ds, Properties props, BufferedReader in)
            throws Exception {
        System.out.print("Enter accountId: ");
        String acc = in.readLine();
        if (acc == null || acc.trim().isEmpty()) acc = "acct123";

        System.out.print("Destination PDF (default alerts_report.pdf): ");
        String out = in.readLine();
        if (out == null || out.trim().isEmpty()) out = "alerts_report.pdf";

        exportReportPdf(ds, acc, out);
    }

    // EXPORT PDF BASE FUNCTION
    private static void exportReportPdf(DataSource ds, String acc, String dest) {
        new ReportService(new AlertDao(ds)).exportAlertsToPdf(acc, dest);
    }

    // --------------------------------------------------------------------
    // MENU: Export JSON
    // --------------------------------------------------------------------
    private static void exportJsonInteractive(DataSource ds, Properties props, BufferedReader in)
            throws Exception {
        System.out.print("Enter accountId: ");
        String acc = in.readLine();
        if (acc == null || acc.trim().isEmpty()) acc = "acct123";

        System.out.print("Destination JSON (default alerts_report.json): ");
        String out = in.readLine();
        if (out == null || out.trim().isEmpty()) out = "alerts_report.json";

        exportReportJson(ds, acc, out);
    }

    // EXPORT JSON BASE FUNCTION
    private static void exportReportJson(DataSource ds, String acc, String dest) {
        new ReportService(new AlertDao(ds)).exportAlertsToJson(acc, dest);
    }

    private static void showConfiguration(ConfigLoader cfg) {
        System.out.println("==== Configuration ====");
        Properties props = cfg.getProperties();
        props.forEach((k, v) -> System.out.println(k + " = " + v));

        System.out.println("\nFraud rule weights from rules.json:");
        cfg.getRuleWeights().forEach((k, v) -> System.out.println(k + " = " + v));

        System.out.println("==== End of Configuration ====");
    }


    // --------------------------------------------------------------------
    // DETECTION PIPELINE
    // --------------------------------------------------------------------
    private static void runDetection(ConfigLoader cfg, DataSource ds) throws Exception {
        Properties p = cfg.getProperties();

        List<Rule> rules = RuleFactory.createRules(cfg);

        DetectionService svc = new DetectionService(
                new TransactionDao(ds),
                new AlertDao(ds),
                rules,
                Integer.parseInt(p.getProperty("risk.score.medium")),
                Integer.parseInt(p.getProperty("risk.score.high")),
                Integer.parseInt(p.getProperty("velocity.window.seconds")),
                Integer.parseInt(p.getProperty("velocity.limit"))
        );

        List<Transaction> txns = CsvReader.readFromResource("/transactions.csv");
        int alerts = 0;

        for (Transaction t : txns) {
            Optional<FraudAlert> result = svc.analyzeAndPersist(t);
            if (result.isPresent()) {
                alerts++;
                log.warn("ALERT: {}", result.get());
            }
        }

        System.out.println("Detection complete. Alerts created: " + alerts);
    }

    // --------------------------------------------------------------------
    // DB TEST
    // --------------------------------------------------------------------
    private static void dbTest(DataSource ds) {
        TransactionDao dao = new TransactionDao(ds);

        try {
            Transaction t = new Transaction(
                    "TEST-" + UUID.randomUUID(),
                    "acct123",
                    999.0,
                    "INR",
                    LocalDateTime.now(),
                    "TEST",
                    "India",
                    "Card"
            );
            dao.save(t);
            System.out.println("Saved: " + t.getTransactionId());
        } catch (Exception e) {
            System.out.println("DB test failed: " + e.getMessage());
        }
    }

    // --------------------------------------------------------------------
    // USAGE
    // --------------------------------------------------------------------
    private static void printUsage() {
        System.out.println("CLI Usage:");
        System.out.println(" run-detection");
        System.out.println(" export-report acct123");
        System.out.println(" export-pdf acct123 report.pdf");
        System.out.println(" export-json acct123 report.json");
        System.out.println(" db-test");
    }
}
