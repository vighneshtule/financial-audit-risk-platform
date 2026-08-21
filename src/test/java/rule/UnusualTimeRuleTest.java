package rule;

import model.RiskFinding;
import model.RiskSeverity;
import model.RiskType;
import model.Transaction;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UnusualTimeRuleTest {

    @Test
    void shouldDetectTransactionBeforeBusinessHours() {

        Transaction transaction = new Transaction(
                "TXN001",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("50000.00"),
                LocalDateTime.of(2026, 8, 20, 2, 30),
                "Office Supplies"
        );

        UnusualTimeRule rule = new UnusualTimeRule();

        RiskFinding finding = rule.evaluate(transaction);

        assertNotNull(finding);

        assertEquals(
                RiskType.UNUSUAL_TRANSACTION_TIME,
                finding.getType()
        );

        assertEquals(
                20,
                finding.getScore()
        );

        assertEquals(
                RiskSeverity.MEDIUM,
                finding.getSeverity()
        );
    }

    @Test
    void shouldNotDetectTransactionDuringBusinessHours() {

        Transaction transaction = new Transaction(
                "TXN002",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("50000.00"),
                LocalDateTime.of(2026, 8, 20, 14, 30),
                "Office Supplies"
        );

        UnusualTimeRule rule = new UnusualTimeRule();

        RiskFinding finding = rule.evaluate(transaction);

        assertNull(finding);
    }

    @Test
    void shouldDetectTransactionAfterBusinessHours() {

        Transaction transaction = new Transaction(
                "TXN003",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("50000.00"),
                LocalDateTime.of(2026, 8, 20, 19, 30),
                "Office Supplies"
        );

        UnusualTimeRule rule = new UnusualTimeRule();

        RiskFinding finding = rule.evaluate(transaction);

        assertNotNull(finding);

        assertEquals(
                RiskType.UNUSUAL_TRANSACTION_TIME,
                finding.getType()
        );

        assertEquals(
                20,
                finding.getScore()
        );
    }
}