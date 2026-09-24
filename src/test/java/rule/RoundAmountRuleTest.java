package rule;

import config.RiskConfiguration;
import model.RiskFinding;
import model.RiskSeverity;
import model.RiskType;
import model.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RoundAmountRuleTest {

    @Test
    void shouldDetectExactMultipleOfUnit() {
        Transaction transaction = new Transaction(
                "TXN001",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("50000.00"),
                LocalDateTime.of(2026, 8, 20, 10, 0),
                "Office Supplies"
        );

        RoundAmountRule rule = new RoundAmountRule();
        RiskFinding finding = rule.evaluate(transaction);

        assertNotNull(finding);
        assertEquals(RiskType.ROUND_AMOUNT, finding.getType());
        assertEquals(10, finding.getScore());
        assertEquals(RiskSeverity.MEDIUM, finding.getSeverity());
        assertEquals("Transaction amount is a round multiple of INR 10000", finding.getExplanation());
    }

    @Test
    void shouldNotDetectNonMultipleOfUnit() {
        Transaction transaction = new Transaction(
                "TXN002",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("15750.00"),
                LocalDateTime.of(2026, 8, 20, 10, 0),
                "Office Supplies"
        );

        RoundAmountRule rule = new RoundAmountRule();
        RiskFinding finding = rule.evaluate(transaction);

        assertNull(finding);
    }

    @Test
    void shouldRespectCustomUnit() {
        Transaction transaction = new Transaction(
                "TXN003",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("25000.00"),
                LocalDateTime.of(2026, 8, 20, 10, 0),
                "Office Supplies"
        );

        RoundAmountRule rule = new RoundAmountRule(new BigDecimal("5000"), 15);
        RiskFinding finding = rule.evaluate(transaction);

        assertNotNull(finding);
        assertEquals(15, finding.getScore());
        assertEquals("Transaction amount is a round multiple of INR 5000", finding.getExplanation());
    }

    @Test
    void shouldRespectCustomScore() {
        Transaction transaction = new Transaction(
                "TXN004",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("100000.00"),
                LocalDateTime.of(2026, 8, 20, 10, 0),
                "Office Supplies"
        );

        RoundAmountRule rule = new RoundAmountRule(new BigDecimal("10000"), 25);
        RiskFinding finding = rule.evaluate(transaction);

        assertNotNull(finding);
        assertEquals(25, finding.getScore());
    }

    @Test
    void shouldCreateFromConfiguration() {
        RiskConfiguration.RoundAmount config = new RiskConfiguration.RoundAmount();
        config.setUnit(new BigDecimal("20000"));
        config.setScore(30);

        RoundAmountRule rule = new RoundAmountRule(config);
        assertEquals(new BigDecimal("20000"), rule.getUnit());
        assertEquals(30, rule.getScore());

        Transaction transaction = new Transaction(
                "TXN005",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("40000.00"),
                LocalDateTime.of(2026, 8, 20, 10, 0),
                "Office Supplies"
        );
        RiskFinding finding = rule.evaluate(transaction);
        assertNotNull(finding);
        assertEquals(30, finding.getScore());
        assertEquals("Transaction amount is a round multiple of INR 20000", finding.getExplanation());
    }

    @Test
    void shouldNotDetectZeroOrNegativeAmount() {
        RoundAmountRule rule = new RoundAmountRule();

        Transaction zeroTxn = new Transaction(
                "TXN006",
                "ABC Suppliers",
                "EMP101",
                BigDecimal.ZERO,
                LocalDateTime.of(2026, 8, 20, 10, 0),
                "Office Supplies"
        );
        assertNull(rule.evaluate(zeroTxn));

        Transaction negativeTxn = new Transaction(
                "TXN007",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("-10000.00"),
                LocalDateTime.of(2026, 8, 20, 10, 0),
                "Office Supplies"
        );
        assertNull(rule.evaluate(negativeTxn));
    }

    @Test
    void shouldHandleNullTransactionOrAmountSafely() {
        RoundAmountRule rule = new RoundAmountRule();

        assertNull(rule.evaluate(null));

        Transaction nullAmountTxn = new Transaction(
                "TXN008",
                "ABC Suppliers",
                "EMP101",
                null,
                LocalDateTime.of(2026, 8, 20, 10, 0),
                "Office Supplies"
        );
        assertNull(rule.evaluate(nullAmountTxn));
    }
}
