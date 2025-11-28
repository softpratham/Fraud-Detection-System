package com.fraud.app;

import com.fraud.config.ConfigLoader;
import com.fraud.dao.TransactionDao;
import com.fraud.util.DBUtil;
import com.fraud.model.Transaction;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

public class DBTest {
    public static void main(String[] args) throws Exception {
        ConfigLoader cfg = new ConfigLoader();
        Properties p = cfg.getProperties();
        DataSource ds = DBUtil.createDataSource(p);
        TransactionDao txDao = new TransactionDao(ds);

        Transaction tx = new Transaction("TEST_TXN_1", "acct123", 1234.5, "INR",
                LocalDateTime.now().minusMinutes(1), "TEST_MERCHANT", "India", "Card");

        System.out.println("Saving test transaction...");
        txDao.save(tx);

        System.out.println("Fetching recent transactions for acct123...");
        List<Transaction> recent = txDao.getRecentTransactions("acct123", 3600);
        recent.forEach(t -> System.out.println(t));
    }
}
