package com.fraud.app;

import com.fraud.config.ConfigLoader;
import com.fraud.dao.AlertDao;
import com.fraud.model.FraudAlert;
import com.fraud.service.ReportService;
import com.fraud.util.DBUtil;
import com.fraud.util.ShutdownUtil;

import javax.sql.DataSource;
import java.util.*;

public class FraudAnalystConsole {

    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {
        ConfigLoader cfg = null;
        DataSource ds = null;

        try {
            System.out.println("===============================");
            System.out.println("      FRAUD ANALYST CONSOLE    ");
            System.out.println("===============================\n");

            cfg = new ConfigLoader();
            Properties props = cfg.getProperties();
            ds = DBUtil.createDataSource(props);

            AlertDao alertDao = new AlertDao(ds);
            ReportService reportService = new ReportService(alertDao);

            showConfig(props);

            boolean running = true;
            while (running) {
                printMenu();
                int choice = readInt("Enter choice: ");

                switch (choice) {
                    case 1:
                        handleOverview(alertDao);
                        break;
                    case 2:
                        handleListAlerts(alertDao);
                        break;
                    case 3:
                        handleRecentAlerts(alertDao);
                        break;
                    case 4:
                        handleExport(alertDao, reportService, props);
                        break;
                    case 0:
                        running = false;
                        System.out.println("Shutting down console...");
                        break;
                    default:
                        System.out.println("Unknown option. Please try again.");
                }
            }

            System.out.println("Bye!");

        } catch (Exception e) {
            System.out.println("ERROR: Analyst console failed!");
            e.printStackTrace();
        } finally {
            // cleanly shutdown pool + JDBC drivers
            ShutdownUtil.closeDataSource(ds);
        }
    }

    // ----- UI helpers -----

    private static void showConfig(Properties props) {
        System.out.println("Loaded configuration:");
        System.out.println("DB URL  : " + props.getProperty("db.url"));
        System.out.println("Reports : " + props.getProperty("report.output", "alerts_report.csv"));
        System.out.println();
    }

    private static void printMenu() {
        System.out.println("--------------------------------");
        System.out.println("1) View alerts overview (by risk)");
        System.out.println("2) List alerts for an account");
        System.out.println("3) View recent alerts (last N)");
        System.out.println("4) Export alerts report (CSV)");
        System.out.println("0) Exit");
        System.out.println("--------------------------------");
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = SCANNER.nextLine().trim();
            if (line.isEmpty()) return 0;
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return SCANNER.nextLine().trim();
    }

    // ----- Menu actions -----

    private static void handleOverview(AlertDao alertDao) {
        String accountId = readLine("Enter accountId (e.g. acct123): ");
        if (accountId.isEmpty()) {
            System.out.println("Account id is required.");
            return;
        }

        int limit = readInt("Max alerts to load (default 50): ");
        if (limit <= 0) limit = 50;

        List<FraudAlert> alerts = alertDao.getAlertsByAccount(accountId, limit);
        if (alerts.isEmpty()) {
            System.out.println("No alerts found for account " + accountId);
            return;
        }

        long total = alerts.size();
        long high = alerts.stream().filter(a -> "HIGH".equalsIgnoreCase(a.getRiskLevel())).count();
        long medium = alerts.stream().filter(a -> "MEDIUM".equalsIgnoreCase(a.getRiskLevel())).count();
        long low = alerts.stream().filter(a -> "LOW".equalsIgnoreCase(a.getRiskLevel())).count();

        System.out.println();
        System.out.println("--- Alerts Overview for account: " + accountId + " ---");
        System.out.println("Total alerts : " + total);
        System.out.println("HIGH  alerts : " + high);
        System.out.println("MEDIUM alerts: " + medium);
        System.out.println("LOW   alerts : " + low);

        System.out.println("\nRecent alerts:");
        int idx = 1;
        for (FraudAlert a : alerts) {
            System.out.printf("[%d] id=%d  txId=%s  risk=%s  score=%d  createdAt=%s%n",
                    idx++, a.getId(), a.getTransactionId(), a.getRiskLevel(),
                    a.getScore(), a.getCreatedAt());
            if (idx > 10) break; // show only first 10
        }
        System.out.println();
    }

    private static void handleListAlerts(AlertDao alertDao) {
        String accountId = readLine("Enter accountId: ");
        if (accountId.isEmpty()) {
            System.out.println("Account id is required.");
            return;
        }
        int limit = readInt("Max alerts to list (default 50): ");
        if (limit <= 0) limit = 50;

        List<FraudAlert> alerts = alertDao.getAlertsByAccount(accountId, limit);
        if (alerts.isEmpty()) {
            System.out.println("No alerts found for account " + accountId);
            return;
        }

        System.out.println("\n--- Alerts for account: " + accountId + " (limit=" + limit + ") ---");
        for (FraudAlert a : alerts) {
            System.out.printf("id=%d  txId=%s  risk=%s  score=%d  createdAt=%s%n",
                    a.getId(), a.getTransactionId(), a.getRiskLevel(),
                    a.getScore(), a.getCreatedAt());
            System.out.println("   reason: " + a.getReason());
        }
        System.out.println();
    }

    private static void handleRecentAlerts(AlertDao alertDao) {
        // This is basically a list view for a chosen account, but highlighted as "recent"
        handleListAlerts(alertDao);
    }

    private static void handleExport(AlertDao alertDao, ReportService reportService, Properties props) {
        String defaultAccount = "acct123";
        String accountId = readLine("Enter accountId to export (default " + defaultAccount + "): ");
        if (accountId.isEmpty()) accountId = defaultAccount;

        String outFile = props.getProperty("report.output", "alerts_report.csv");
        reportService.exportAlerts(accountId, outFile);
        System.out.println("Report exported successfully: " + outFile);
        System.out.println();
    }
}
