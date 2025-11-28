package com.fraud;

import com.fraud.dao.AlertDao;
import com.fraud.dao.TransactionDao;
import com.fraud.model.FraudAlert;
import com.fraud.model.Transaction;
import com.fraud.rules.Rule;
import com.fraud.rules.RuleResult;
import com.fraud.service.DetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class DetectionServiceTest {

    private TransactionDao txDao;
    private AlertDao alertDao;

    @BeforeEach
    public void setup() {
        txDao = mock(TransactionDao.class);
        alertDao = mock(AlertDao.class);
    }

    @Test
    public void testLowRiskTransactionDoesNotCreateAlert() {
        // create a rule that returns zero score
        Rule zeroRule = new Rule() {
            @Override
            public RuleResult evaluate(Transaction txn) {
                return new RuleResult(name(), false, 0, "ok");
            }
            @Override public String name() { return "zeroRule"; }
        };

        when(txDao.getRecentTransactions(anyString(), anyInt())).thenReturn(Collections.emptyList());

        DetectionService svc = new DetectionService(txDao, alertDao,
                Collections.singletonList(zeroRule),
                30, 60,
                120, 3);

        Transaction t = new Transaction("T1", "acct1", 10.0, "INR",
                LocalDateTime.now(), "M", "India", "Card");

        Optional<FraudAlert> opt = svc.analyzeAndPersist(t);
        assertFalse(opt.isPresent());
        verify(txDao, times(1)).save(t);
        verify(alertDao, never()).saveAlert(any(FraudAlert.class));
    }

    @Test
    public void testHighRiskTransactionCreatesAlert() {
        // rule that matches with high contribution
        Rule highRule = new Rule() {
            @Override
            public RuleResult evaluate(Transaction txn) {
                return new RuleResult(name(), true, 50, "big");
            }

            @Override
            public String name() { return "highRule"; }
        };

        when(txDao.getRecentTransactions(anyString(), anyInt())).thenReturn(Collections.emptyList());

        DetectionService svc = new DetectionService(txDao, alertDao,
                Collections.singletonList(highRule),
                30, 60,
                120, 3);

        Transaction t = new Transaction("T2", "acct1", 1000.0, "INR",
                LocalDateTime.now(), "M", "India", "Card");

        Optional<FraudAlert> opt = svc.analyzeAndPersist(t);
        assertTrue(opt.isPresent());
        verify(txDao, times(1)).save(t);
        verify(alertDao, times(1)).saveAlert(any(FraudAlert.class));
    }
}
