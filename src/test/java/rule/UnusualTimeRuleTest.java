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

    @Test
    void shouldRespectCustomBusinessHoursAndScore() {

        // Custom hours: 10:00 to 16:00, custom score: 35
        UnusualTimeRule rule = new UnusualTimeRule(
                java.time.LocalTime.of(10, 0),
                java.time.LocalTime.of(16, 0),
                35
        );

        // 09:30 is outside 10:00 - 16:00 (under default 09:00 - 18:00 it would have been within business hours)
        Transaction transactionAt930 = new Transaction(
                "TXN004",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("50000.00"),
                LocalDateTime.of(2026, 8, 20, 9, 30),
                "Office Supplies"
        );

        RiskFinding finding = rule.evaluate(transactionAt930);

        assertNotNull(finding);
        assertEquals(35, finding.getScore());
        assertEquals(RiskType.UNUSUAL_TRANSACTION_TIME, finding.getType());

        // 12:00 is within 10:00 - 16:00
        Transaction transactionAt1200 = new Transaction(
                "TXN005",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("50000.00"),
                LocalDateTime.of(2026, 8, 20, 12, 0),
                "Office Supplies"
        );

        assertNull(rule.evaluate(transactionAt1200));
    }
}