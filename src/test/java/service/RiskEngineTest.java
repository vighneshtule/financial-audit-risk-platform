package service;

import model.RiskReport;
import model.RiskSeverity;
import model.RiskType;
import model.Transaction;
import org.junit.jupiter.api.Test;
import rule.DuplicateTransactionRule;
import rule.HighAmountRule;
import rule.UnusualTimeRule;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiskEngineTest {

    @Test
    void shouldReturnLowRiskForNormalTransaction() {

        LocalDateTime time =
                LocalDateTime.of(2026, 8, 20, 14, 30);

        Transaction transaction = new Transaction(
                "TXN100",
                "Normal Vendor",
                "EMP200",
                50_000,
                time,
                "Office Supplies"
        );

        List<Transaction> transactions =
                List.of(transaction);

        RiskEngine engine = new RiskEngine();

        engine.addRule(new HighAmountRule());
        engine.addRule(new UnusualTimeRule());

        engine.addDatasetRule(
                new DuplicateTransactionRule()
        );

        RiskReport report =
                engine.analyze(transaction, transactions);

        assertEquals(0, report.getRiskScore());

        assertEquals(
                RiskSeverity.LOW,
                report.getRiskLevel()
        );

        assertTrue(
                report.getFindings().isEmpty()
        );
    }

    @Test
    void shouldCombineMultipleRiskRules() {

        LocalDateTime baseTime =
                LocalDateTime.of(2026, 8, 20, 2, 30);

        Transaction t1 = new Transaction(
                "TXN001",
                "ABC Suppliers",
                "EMP101",
                150_000,
                baseTime,
                "Office Supplies"
        );

        Transaction t2 = new Transaction(
                "TXN002",
                "ABC Suppliers",
                "EMP101",
                150_000,
                baseTime.plusMinutes(5),
                "Office Supplies"
        );

        List<Transaction> transactions =
                List.of(t1, t2);

        RiskEngine engine = new RiskEngine();

        engine.addRule(new HighAmountRule());

        engine.addRule(new UnusualTimeRule());

        engine.addDatasetRule(
                new DuplicateTransactionRule()
        );

        RiskReport report =
                engine.analyze(t1, transactions);

        // 30 High Amount
        // 20 Unusual Time
        // 25 Duplicate
        // Total = 75

        assertEquals(
                75,
                report.getRiskScore()
        );

        assertEquals(
                RiskSeverity.HIGH,
                report.getRiskLevel()
        );

        assertEquals(
                3,
                report.getFindings().size()
        );

        assertTrue(
                report.getFindings()
                        .stream()
                        .anyMatch(f ->
                                f.getType()
                                        == RiskType.HIGH_AMOUNT)
        );

        assertTrue(
                report.getFindings()
                        .stream()
                        .anyMatch(f ->
                                f.getType()
                                        == RiskType.UNUSUAL_TRANSACTION_TIME)
        );

        assertTrue(
                report.getFindings()
                        .stream()
                        .anyMatch(f ->
                                f.getType()
                                        == RiskType.DUPLICATE_TRANSACTION)
        );
    }
}