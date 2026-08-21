package rule;

import model.RiskFinding;
import model.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}