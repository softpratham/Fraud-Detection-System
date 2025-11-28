package com.fraud.util;

import com.fraud.model.Transaction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CsvReader {

    /**
     * Reads transactions CSV from resources or given InputStream.
     * Expects header: transactionId,accountId,amount,currency,timestamp,merchant,location,channel
     */
    public static List<Transaction> readFromResource(String resourcePath) throws Exception {
        try (InputStream is = CsvReader.class.getResourceAsStream(resourcePath)) {
            if (is == null) throw new IllegalArgumentException("Resource not found: " + resourcePath);
            return read(is);
        }
    }

    public static List<Transaction> read(InputStream inputStream) throws Exception {
        List<Transaction> list = new ArrayList<>();
        try (Reader in = new InputStreamReader(inputStream)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreEmptyLines()
                    .withTrim()
                    .parse(in);
            for (CSVRecord r : records) {
                String txIdField = r.get("transactionId");
                if (txIdField == null || txIdField.trim().isEmpty()) continue;

                String transactionId = txIdField.trim();
                String accountId = r.get("accountId").trim();
                double amount = Double.parseDouble(r.get("amount").trim());
                String currency = r.get("currency").trim();
                String timestamp = r.get("timestamp").trim();
                String merchant = r.get("merchant").trim();
                String location = r.get("location").trim();
                String channel = r.get("channel").trim();

                LocalDateTime ts = DateUtil.parse(timestamp);

                Transaction t = new Transaction(transactionId, accountId, amount, currency, ts, merchant, location, channel);
                list.add(t);
            }
        }
        return list;
    }
}
