package rule;

import model.RiskFinding;
import model.RiskType;
import model.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class DuplicateTransactionRuleTest {

    @Test
    void shouldDetectDuplicateTransactionsWithinTenMinutes() {

        LocalDateTime baseTime =
                LocalDateTime.of(2026, 8, 20, 10, 0);

        Transaction t1 = new Transaction(
                "TXN001",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("50000.00"),
                baseTime,
                "Office Supplies"
        );

        Transaction t2 = new Transaction(
                "TXN002",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("50000.00"),
                baseTime.plusMinutes(5),
                "Office Supplies"
        );

        List<Transaction> transactions =
                List.of(t1, t2);

        DuplicateTransactionRule rule =
                new DuplicateTransactionRule();

        RiskFinding finding =
                rule.evaluate(t1, transactions);

        assertEquals(25, finding.getScore());
    }

    @Test
    void shouldNotDetectDuplicateTransactionsOutsideTenMinutes() {

        LocalDateTime baseTime =
                LocalDateTime.of(2026, 8, 20, 10, 0);

        Transaction t1 = new Transaction(
                "TXN001",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("50000.00"),
                baseTime,
                "Office Supplies"
        );

        Transaction t2 = new Transaction(
                "TXN002",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("50000.00"),
                baseTime.plusMinutes(20),
                "Office Supplies"
        );

        List<Transaction> transactions =
                List.of(t1, t2);

        DuplicateTransactionRule rule =
                new DuplicateTransactionRule();

        RiskFinding finding =
                rule.evaluate(t1, transactions);

        assertNull(finding);
    }

    @Test
    void shouldNotDetectDuplicateWhenVendorIsDifferent() {

        LocalDateTime baseTime =
                LocalDateTime.of(2026, 8, 20, 10, 0);

        Transaction t1 = new Transaction(
                "TXN001",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("50000.00"),
                baseTime,
                "Office Supplies"
        );

        Transaction t2 = new Transaction(
                "TXN002",
                "XYZ Traders",
                "EMP101",
                new BigDecimal("50000.00"),
                baseTime.plusMinutes(5),
                "Office Supplies"
        );

        List<Transaction> transactions =
                List.of(t1, t2);

        DuplicateTransactionRule rule =
                new DuplicateTransactionRule();

        RiskFinding finding =
                rule.evaluate(t1, transactions);

        assertNull(finding);
    }

    @Test
    void shouldNotDetectDuplicateWhenAmountIsDifferent() {

        LocalDateTime baseTime =
                LocalDateTime.of(2026, 8, 20, 10, 0);

        Transaction t1 = new Transaction(
                "TXN001",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("50000.00"),
                baseTime,
                "Office Supplies"
        );

        Transaction t2 = new Transaction(
                "TXN002",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("20000.00"),
                baseTime.plusMinutes(5),
                "Office Supplies"
        );

        List<Transaction> transactions =
                List.of(t1, t2);

        DuplicateTransactionRule rule =
                new DuplicateTransactionRule();

        RiskFinding finding =
                rule.evaluate(t1, transactions);

        assertNull(finding);
    }

    @Test
    void shouldNotDetectTransactionAsItsOwnDuplicate() {

        LocalDateTime time =
                LocalDateTime.of(2026, 8, 20, 10, 0);

        Transaction transaction = new Transaction(
                "TXN001",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("50000.00"),
                time,
                "Office Supplies"
        );

        List<Transaction> transactions =
                List.of(transaction);

        DuplicateTransactionRule rule =
                new DuplicateTransactionRule();

        RiskFinding finding =
                rule.evaluate(transaction, transactions);

        assertNull(finding);
    }

    @Test
    void shouldRespectCustomDuplicateWindowAndScore() {

        LocalDateTime baseTime =
                LocalDateTime.of(2026, 8, 20, 10, 0);

        Transaction t1 = new Transaction(
                "TXN001",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("50000.00"),
                baseTime,
                "Office Supplies"
        );

        // 25 minutes later (default 10 min would ignore this, but custom 30 min window will catch it)
        Transaction t2 = new Transaction(
                "TXN002",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("50000.00"),
                baseTime.plusMinutes(25),
                "Office Supplies"
        );

        List<Transaction> transactions = List.of(t1, t2);

        // Custom window: 30 minutes, custom score: 50
        DuplicateTransactionRule customRule =
                new DuplicateTransactionRule(30, 50);

        RiskFinding finding = customRule.evaluate(t1, transactions);

        assertNotNull(finding);
        assertEquals(50, finding.getScore());
        assertEquals(RiskType.DUPLICATE_TRANSACTION, finding.getType());

        // Under default rule (10 minutes), t2 (25 mins later) is not detected
        DuplicateTransactionRule defaultRule = new DuplicateTransactionRule();
        assertNull(defaultRule.evaluate(t1, transactions));
    }
}