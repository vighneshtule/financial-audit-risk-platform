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

class VendorConcentrationRuleTest {

    @Test
    void shouldDetectWhenConcentrationReachesThreshold() {
        LocalDateTime time = LocalDateTime.of(2026, 8, 20, 10, 0);

        // EMP101 has 4 transactions: 3 with ABC Suppliers (75%), 1 with XYZ Traders (25%)
        Transaction t1 = new Transaction("TXN001", "ABC Suppliers", "EMP101", new BigDecimal("1000.00"), time, "Supplies");
        Transaction t2 = new Transaction("TXN002", "ABC Suppliers", "EMP101", new BigDecimal("2000.00"), time.plusHours(1), "Supplies");
        Transaction t3 = new Transaction("TXN003", "ABC Suppliers", "EMP101", new BigDecimal("3000.00"), time.plusHours(2), "Supplies");
        Transaction t4 = new Transaction("TXN004", "XYZ Traders", "EMP101", new BigDecimal("4000.00"), time.plusHours(3), "Supplies");

        List<Transaction> dataset = List.of(t1, t2, t3, t4);
        VendorConcentrationRule rule = new VendorConcentrationRule();

        RiskFinding finding = rule.evaluate(t1, dataset);

        assertNotNull(finding);
        assertEquals(RiskType.VENDOR_CONCENTRATION, finding.getType());
        assertEquals(15, finding.getScore());
        assertEquals(RiskSeverity.MEDIUM, finding.getSeverity());
        assertEquals("Employee transactions are 75% concentrated with vendor ABC Suppliers", finding.getExplanation());
    }

    @Test
    void shouldNotDetectWhenConcentrationBelowThreshold() {
        LocalDateTime time = LocalDateTime.of(2026, 8, 20, 10, 0);

        // EMP101 has 4 transactions: 2 with ABC Suppliers (50%), 2 with XYZ Traders (50%)
        Transaction t1 = new Transaction("TXN001", "ABC Suppliers", "EMP101", new BigDecimal("1000.00"), time, "Supplies");
        Transaction t2 = new Transaction("TXN002", "ABC Suppliers", "EMP101", new BigDecimal("2000.00"), time.plusHours(1), "Supplies");
        Transaction t3 = new Transaction("TXN003", "XYZ Traders", "EMP101", new BigDecimal("3000.00"), time.plusHours(2), "Supplies");
        Transaction t4 = new Transaction("TXN004", "XYZ Traders", "EMP101", new BigDecimal("4000.00"), time.plusHours(3), "Supplies");

        List<Transaction> dataset = List.of(t1, t2, t3, t4);
        VendorConcentrationRule rule = new VendorConcentrationRule();

        assertNull(rule.evaluate(t1, dataset));
    }

    @Test
    void shouldIgnoreDifferentEmployeeTransactions() {
        LocalDateTime time = LocalDateTime.of(2026, 8, 20, 10, 0);

        // EMP101 has only 2 transactions with ABC Suppliers. Other transactions belong to EMP102.
        Transaction t1 = new Transaction("TXN001", "ABC Suppliers", "EMP101", new BigDecimal("1000.00"), time, "Supplies");
        Transaction t2 = new Transaction("TXN002", "ABC Suppliers", "EMP101", new BigDecimal("2000.00"), time.plusHours(1), "Supplies");
        Transaction t3 = new Transaction("TXN003", "ABC Suppliers", "EMP102", new BigDecimal("3000.00"), time.plusHours(2), "Supplies");
        Transaction t4 = new Transaction("TXN004", "ABC Suppliers", "EMP102", new BigDecimal("4000.00"), time.plusHours(3), "Supplies");

        List<Transaction> dataset = List.of(t1, t2, t3, t4);
        VendorConcentrationRule rule = new VendorConcentrationRule();

        // Below minimum transactions (2 < 3) for EMP101
        assertNull(rule.evaluate(t1, dataset));
    }

    @Test
    void shouldEnforceMinimumTransactionsRequirement() {
        LocalDateTime time = LocalDateTime.of(2026, 8, 20, 10, 0);

        // EMP101 has 2 transactions with ABC Suppliers (100% concentration, but minimum is 3)
        Transaction t1 = new Transaction("TXN001", "ABC Suppliers", "EMP101", new BigDecimal("1000.00"), time, "Supplies");
        Transaction t2 = new Transaction("TXN002", "ABC Suppliers", "EMP101", new BigDecimal("2000.00"), time.plusHours(1), "Supplies");

        List<Transaction> dataset = List.of(t1, t2);
        VendorConcentrationRule rule = new VendorConcentrationRule();

        assertNull(rule.evaluate(t1, dataset));
    }

    @Test
    void shouldRespectCustomPercentageThreshold() {
        LocalDateTime time = LocalDateTime.of(2026, 8, 20, 10, 0);

        // EMP101 has 5 transactions: 3 with ABC Suppliers (60%), 2 with XYZ Traders (40%)
        Transaction t1 = new Transaction("TXN001", "ABC Suppliers", "EMP101", new BigDecimal("1000.00"), time, "Supplies");
        Transaction t2 = new Transaction("TXN002", "ABC Suppliers", "EMP101", new BigDecimal("2000.00"), time.plusHours(1), "Supplies");
        Transaction t3 = new Transaction("TXN003", "ABC Suppliers", "EMP101", new BigDecimal("3000.00"), time.plusHours(2), "Supplies");
        Transaction t4 = new Transaction("TXN004", "XYZ Traders", "EMP101", new BigDecimal("4000.00"), time.plusHours(3), "Supplies");
        Transaction t5 = new Transaction("TXN005", "XYZ Traders", "EMP101", new BigDecimal("5000.00"), time.plusHours(4), "Supplies");

        List<Transaction> dataset = List.of(t1, t2, t3, t4, t5);
        // Custom threshold 55%, min 3 txns, score 20
        VendorConcentrationRule rule = new VendorConcentrationRule(55.0, 3, 20);

        RiskFinding finding = rule.evaluate(t1, dataset);

        assertNotNull(finding);
        assertEquals(20, finding.getScore());
        assertEquals("Employee transactions are 60% concentrated with vendor ABC Suppliers", finding.getExplanation());
    }

    @Test
    void shouldRespectCustomMinimumTransactionCount() {
        LocalDateTime time = LocalDateTime.of(2026, 8, 20, 10, 0);

        Transaction t1 = new Transaction("TXN001", "ABC Suppliers", "EMP101", new BigDecimal("1000.00"), time, "Supplies");
        Transaction t2 = new Transaction("TXN002", "ABC Suppliers", "EMP101", new BigDecimal("2000.00"), time.plusHours(1), "Supplies");

        List<Transaction> dataset = List.of(t1, t2);
        // Custom min transactions = 2, threshold = 80%, score = 15
        VendorConcentrationRule rule = new VendorConcentrationRule(80.0, 2, 15);

        RiskFinding finding = rule.evaluate(t1, dataset);

        assertNotNull(finding);
        assertEquals("Employee transactions are 100% concentrated with vendor ABC Suppliers", finding.getExplanation());
    }

    @Test
    void shouldRespectCustomScore() {
        LocalDateTime time = LocalDateTime.of(2026, 8, 20, 10, 0);

        Transaction t1 = new Transaction("TXN001", "ABC Suppliers", "EMP101", new BigDecimal("1000.00"), time, "Supplies");
        Transaction t2 = new Transaction("TXN002", "ABC Suppliers", "EMP101", new BigDecimal("2000.00"), time.plusHours(1), "Supplies");
        Transaction t3 = new Transaction("TXN003", "ABC Suppliers", "EMP101", new BigDecimal("3000.00"), time.plusHours(2), "Supplies");

        List<Transaction> dataset = List.of(t1, t2, t3);
        VendorConcentrationRule rule = new VendorConcentrationRule(70.0, 3, 40);

        RiskFinding finding = rule.evaluate(t1, dataset);

        assertNotNull(finding);
        assertEquals(40, finding.getScore());
    }

    @Test
    void shouldCreateFromConfiguration() {
        RiskConfiguration.VendorConcentration config = new RiskConfiguration.VendorConcentration();
        config.setThresholdPercent(80.0);
        config.setMinimumTransactions(4);
        config.setScore(25);

        VendorConcentrationRule rule = new VendorConcentrationRule(config);
        assertEquals(80.0, rule.getThresholdPercent());
        assertEquals(4, rule.getMinimumTransactions());
        assertEquals(25, rule.getScore());
    }

    @Test
    void shouldHandleNullOrEmptyDataSafely() {
        VendorConcentrationRule rule = new VendorConcentrationRule();
        Transaction t1 = new Transaction("TXN001", "ABC Suppliers", "EMP101", new BigDecimal("1000.00"), LocalDateTime.now(), "Supplies");

        assertNull(rule.evaluate(null, List.of(t1)));
        assertNull(rule.evaluate(t1, null));
        assertNull(rule.evaluate(t1, Collections.emptyList()));

        Transaction nullEmp = new Transaction("TXN002", "ABC Suppliers", null, new BigDecimal("1000.00"), LocalDateTime.now(), "Supplies");
        assertNull(rule.evaluate(nullEmp, List.of(t1, nullEmp)));

        Transaction nullVendor = new Transaction("TXN003", null, "EMP101", new BigDecimal("1000.00"), LocalDateTime.now(), "Supplies");
        assertNull(rule.evaluate(nullVendor, List.of(nullVendor)));
    }
}
