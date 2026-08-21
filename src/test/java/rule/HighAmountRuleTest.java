package rule;

import model.RiskFinding;
import model.RiskType;
import model.RiskSeverity;
import model.Transaction;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class HighAmountRuleTest {

    @Test
    void shouldDetectHighAmountTransaction() {

        Transaction transaction = new Transaction(
                "TXN001",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("150000.00"),
                LocalDateTime.of(2026, 8, 20, 10, 0),
                "Office Supplies"
        );

        HighAmountRule rule = new HighAmountRule();

        RiskFinding finding = rule.evaluate(transaction);

        assertNotNull(finding);

        assertEquals(
                RiskType.HIGH_AMOUNT,
                finding.getType()
        );

        assertEquals(
                30,
                finding.getScore()
        );

        assertEquals(
                RiskSeverity.MEDIUM,
                finding.getSeverity()
        );
    }

    @Test
    void shouldNotDetectNormalAmountTransaction() {

        Transaction transaction = new Transaction(
                "TXN002",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("50000.00"),
                LocalDateTime.of(2026, 8, 20, 10, 0),
                "Office Supplies"
        );

        HighAmountRule rule = new HighAmountRule();

        RiskFinding finding = rule.evaluate(transaction);

        assertNull(finding);
    }
}