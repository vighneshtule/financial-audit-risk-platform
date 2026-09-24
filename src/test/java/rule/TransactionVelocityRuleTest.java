package rule;

import config.RiskConfiguration;
import model.RiskFinding;
import model.RiskSeverity;
import model.RiskType;
import model.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionVelocityRuleTest {

    @Test
    void shouldDetectWhenThresholdReached() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 8, 20, 10, 0);

        Transaction t1 = new Transaction("TXN001", "ABC Suppliers", "EMP101", new BigDecimal("1000.00"), baseTime, "Supplies");
        Transaction t2 = new Transaction("TXN002", "XYZ Traders", "EMP101", new BigDecimal("2000.00"), baseTime.plusMinutes(10), "Supplies");
        Transaction t3 = new Transaction("TXN003", "Office World", "EMP101", new BigDecimal("3000.00"), baseTime.plusMinutes(20), "Supplies");

        List<Transaction> dataset = List.of(t1, t2, t3);
        TransactionVelocityRule rule = new TransactionVelocityRule();

        RiskFinding finding = rule.evaluate(t1, dataset);

        assertNotNull(finding);
        assertEquals(RiskType.TRANSACTION_VELOCITY, finding.getType());
        assertEquals(20, finding.getScore());
        assertEquals(RiskSeverity.MEDIUM, finding.getSeverity());
        assertEquals("Employee has 3 or more transactions within a 30-minute window", finding.getExplanation());
    }

    @Test
    void shouldNotDetectWhenBelowThreshold() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 8, 20, 10, 0);

        Transaction t1 = new Transaction("TXN001", "ABC Suppliers", "EMP101", new BigDecimal("1000.00"), baseTime, "Supplies");
        Transaction t2 = new Transaction("TXN002", "XYZ Traders", "EMP101", new BigDecimal("2000.00"), baseTime.plusMinutes(10), "Supplies");

        List<Transaction> dataset = List.of(t1, t2);
        TransactionVelocityRule rule = new TransactionVelocityRule();

        RiskFinding finding = rule.evaluate(t1, dataset);

        assertNull(finding);
    }

    @Test
    void shouldIgnoreDifferentEmployee() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 8, 20, 10, 0);

        Transaction t1 = new Transaction("TXN001", "ABC Suppliers", "EMP101", new BigDecimal("1000.00"), baseTime, "Supplies");
        Transaction t2 = new Transaction("TXN002", "XYZ Traders", "EMP102", new BigDecimal("2000.00"), baseTime.plusMinutes(5), "Supplies");
        Transaction t3 = new Transaction("TXN003", "Office World", "EMP103", new BigDecimal("3000.00"), baseTime.plusMinutes(10), "Supplies");

        List<Transaction> dataset = List.of(t1, t2, t3);
        TransactionVelocityRule rule = new TransactionVelocityRule();

        assertNull(rule.evaluate(t1, dataset));
    }

    @Test
    void shouldIgnoreTransactionsOutsideTimeWindow() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 8, 20, 10, 0);

        Transaction t1 = new Transaction("TXN001", "ABC Suppliers", "EMP101", new BigDecimal("1000.00"), baseTime, "Supplies");
        Transaction t2 = new Transaction("TXN002", "XYZ Traders", "EMP101", new BigDecimal("2000.00"), baseTime.plusMinutes(10), "Supplies");
        Transaction t3 = new Transaction("TXN003", "Office World", "EMP101", new BigDecimal("3000.00"), baseTime.plusMinutes(45), "Supplies");

        List<Transaction> dataset = List.of(t1, t2, t3);
        TransactionVelocityRule rule = new TransactionVelocityRule();

        assertNull(rule.evaluate(t1, dataset));
    }

    @Test
    void shouldIncludeCurrentTransactionInCount() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 8, 20, 10, 0);

        Transaction t1 = new Transaction("TXN001", "ABC Suppliers", "EMP101", new BigDecimal("1000.00"), baseTime, "Supplies");
        Transaction t2 = new Transaction("TXN002", "XYZ Traders", "EMP101", new BigDecimal("2000.00"), baseTime.plusMinutes(10), "Supplies");
        Transaction t3 = new Transaction("TXN003", "Office World", "EMP101", new BigDecimal("3000.00"), baseTime.plusMinutes(20), "Supplies");

        // When evaluating t2, t1 (10 mins before) and t3 (10 mins after) and t2 itself are counted => count = 3
        List<Transaction> dataset = List.of(t1, t2, t3);
        TransactionVelocityRule rule = new TransactionVelocityRule();

        RiskFinding finding = rule.evaluate(t2, dataset);
        assertNotNull(finding);
        assertEquals(20, finding.getScore());
    }

    @Test
    void shouldRespectCustomWindowAndThreshold() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 8, 20, 10, 0);

        Transaction t1 = new Transaction("TXN001", "ABC Suppliers", "EMP101", new BigDecimal("1000.00"), baseTime, "Supplies");
        Transaction t2 = new Transaction("TXN002", "XYZ Traders", "EMP101", new BigDecimal("2000.00"), baseTime.plusMinutes(45), "Supplies");

        List<Transaction> dataset = List.of(t1, t2);
        // Custom window: 60 min, threshold: 2, score: 35
        TransactionVelocityRule rule = new TransactionVelocityRule(60, 2, 35);

        RiskFinding finding = rule.evaluate(t1, dataset);

        assertNotNull(finding);
        assertEquals(35, finding.getScore());
        assertEquals("Employee has 2 or more transactions within a 60-minute window", finding.getExplanation());
    }

    @Test
    void shouldCreateFromConfiguration() {
        RiskConfiguration.TransactionVelocity config = new RiskConfiguration.TransactionVelocity();
        config.setWindowMinutes(15);
        config.setThreshold(2);
        config.setScore(40);

        TransactionVelocityRule rule = new TransactionVelocityRule(config);
        assertEquals(15, rule.getWindowMinutes());
        assertEquals(2, rule.getThreshold());
        assertEquals(40, rule.getScore());

        LocalDateTime baseTime = LocalDateTime.of(2026, 8, 20, 10, 0);
        Transaction t1 = new Transaction("TXN001", "ABC Suppliers", "EMP101", new BigDecimal("1000.00"), baseTime, "Supplies");
        Transaction t2 = new Transaction("TXN002", "XYZ Traders", "EMP101", new BigDecimal("2000.00"), baseTime.plusMinutes(10), "Supplies");

        RiskFinding finding = rule.evaluate(t1, List.of(t1, t2));
        assertNotNull(finding);
        assertEquals(40, finding.getScore());
        assertEquals("Employee has 2 or more transactions within a 15-minute window", finding.getExplanation());
    }

    @Test
    void shouldHandleNullOrEmptyDatasetSafely() {
        TransactionVelocityRule rule = new TransactionVelocityRule();
        Transaction t1 = new Transaction("TXN001", "ABC Suppliers", "EMP101", new BigDecimal("1000.00"), LocalDateTime.now(), "Supplies");

        assertNull(rule.evaluate(null, List.of(t1)));
        assertNull(rule.evaluate(t1, null));
        assertNull(rule.evaluate(t1, Collections.emptyList()));

        Transaction nullEmp = new Transaction("TXN002", "ABC Suppliers", null, new BigDecimal("1000.00"), LocalDateTime.now(), "Supplies");
        assertNull(rule.evaluate(nullEmp, List.of(t1, nullEmp)));

        Transaction nullTime = new Transaction("TXN003", "ABC Suppliers", "EMP101", new BigDecimal("1000.00"), null, "Supplies");
        assertNull(rule.evaluate(nullTime, List.of(t1, nullTime)));
    }
}
