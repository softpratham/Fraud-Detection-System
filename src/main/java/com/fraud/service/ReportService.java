package com.fraud.service;

import com.fraud.dao.AlertDao;
import com.fraud.model.FraudAlert;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private static final DateTimeFormatter CREATED_AT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int DEFAULT_LIMIT = 1000;

    private final AlertDao alertDao;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public ReportService(AlertDao alertDao) {
        this.alertDao = Objects.requireNonNull(alertDao, "alertDao required");
    }

    /**
     * Export alerts as CSV. Returns the File written.
     *
     * @param accountId  account to export (non-null)
     * @param outputFile path to write (will be created)
     * @param limit      max number of alerts (pass <=0 for default)
     * @return File that was written
     */
    public File exportAlerts(String accountId, String outputFile, int limit) {
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (limit <= 0) limit = DEFAULT_LIMIT;
        log.info("Exporting CSV report for account={} limit={} -> file={}", accountId, limit, outputFile);

        List<FraudAlert> alerts = alertDao.getAlertsByAccount(accountId, limit);
        if (alerts == null) alerts = Collections.emptyList();

        File out = ensureParentAndFile(outputFile);

        try (BufferedWriter writer = Files.newBufferedWriter(out.toPath(), StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer,
                     CSVFormat.DEFAULT.withHeader("id", "transactionId", "accountId", "score", "riskLevel", "reason", "createdAt"))
        ) {
            for (FraudAlert a : alerts) {
                printer.printRecord(
                        a.getId(),
                        a.getTransactionId(),
                        a.getAccountId(),
                        a.getScore(),
                        a.getRiskLevel(),
                        a.getReason(),
                        a.getCreatedAt() == null ? "" : a.getCreatedAt().format(CREATED_AT_FMT)
                );
            }
            printer.flush();
            log.info("CSV report exported successfully: {} ({} alerts)", out.getAbsolutePath(), alerts.size());
            return out;
        } catch (IOException e) {
            log.error("Failed to export alerts report to CSV: {}", outputFile, e);
            throw new RuntimeException("Failed to export alerts report", e);
        }
    }

    /** Convenience overload keeping previous signature (no limit). */
    public File exportAlerts(String accountId, String outputFile) {
        return exportAlerts(accountId, outputFile, DEFAULT_LIMIT);
    }

    /**
     * Export alerts as JSON array to file.
     *
     * @param accountId  account to export
     * @param outputFile destination path
     * @param limit      max number of alerts (<=0 => default)
     * @return File written
     */
    public File exportAlertsToJson(String accountId, String outputFile, int limit) {
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (limit <= 0) limit = DEFAULT_LIMIT;
        log.info("Exporting JSON report for account={} limit={} -> file={}", accountId, limit, outputFile);

        List<FraudAlert> alerts = alertDao.getAlertsByAccount(accountId, limit);
        if (alerts == null) alerts = Collections.emptyList();

        File out = ensureParentAndFile(outputFile);

        try {
            objectMapper.writeValue(out, alerts);
            log.info("JSON report exported successfully: {} ({} alerts)", out.getAbsolutePath(), alerts.size());
            return out;
        } catch (IOException e) {
            log.error("Failed to export alerts report to JSON", e);
            throw new RuntimeException("Failed to export alerts report (JSON)", e);
        }
    }

    /** Convenience overload (no limit param). */
    public File exportAlertsToJson(String accountId, String outputFile) {
        return exportAlertsToJson(accountId, outputFile, DEFAULT_LIMIT);
    }

    /**
     * Export alerts as simple PDF table using iText7.
     * Returns File written.
     *
     * @param accountId account id
     * @param destPath  destination file path
     * @param limit     max rows
     * @return File written
     */
    public File exportAlertsToPdf(String accountId, String destPath, int limit) {
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (limit <= 0) limit = DEFAULT_LIMIT;
        log.info("Exporting PDF report for account={} limit={} -> file={}", accountId, limit, destPath);

        List<FraudAlert> alerts = alertDao.getAlertsByAccount(accountId, limit);
        if (alerts == null) alerts = Collections.emptyList();

        File out = ensureParentAndFile(destPath);

        try (OutputStream os = Files.newOutputStream(out.toPath())) {
            PdfWriter writer = new PdfWriter(os);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Fraud Detection Report").setBold().setFontSize(16));
            document.add(new Paragraph("Account ID: " + accountId));
            document.add(new Paragraph("Generated on: " + java.time.LocalDateTime.now().format(CREATED_AT_FMT)));
            document.add(new Paragraph("\n"));

            Table table = new Table(UnitValue.createPercentArray(new float[]{1, 3, 2, 6}))
                    .useAllAvailableWidth();

            table.addHeaderCell("ID");
            table.addHeaderCell("Transaction ID");
            table.addHeaderCell("Risk Level");
            table.addHeaderCell("Reason");

            for (FraudAlert alert : alerts) {
                table.addCell(String.valueOf(alert.getId()));
                table.addCell(safeString(alert.getTransactionId()));
                table.addCell(safeString(alert.getRiskLevel()));
                table.addCell(safeString(alert.getReason()));
            }

            document.add(table);
            document.close();

            log.info("PDF Report generated: {} ({} alerts)", out.getAbsolutePath(), alerts.size());
            return out;
        } catch (Exception e) {
            log.error("Failed to generate PDF report", e);
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    public File exportAlertsToPdf(String accountId, String destPath) {
        return exportAlertsToPdf(accountId, destPath, DEFAULT_LIMIT);
    }

    // -----------------------
    // Helper utilities
    // -----------------------
    private File ensureParentAndFile(String path) {
        try {
            File f = new File(path);
            File parent = f.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                log.warn("Could not create parent directories for path: {}", path);
            }
            // If file doesn't exist, create empty file to validate write permission
            if (!f.exists()) f.createNewFile();
            return f;
        } catch (IOException e) {
            log.error("Unable to prepare output file: {}", path, e);
            throw new RuntimeException("Unable to prepare output file: " + path, e);
        }
    }

    private String safeString(Object o) {
        return o == null ? "" : o.toString();
    }
}
