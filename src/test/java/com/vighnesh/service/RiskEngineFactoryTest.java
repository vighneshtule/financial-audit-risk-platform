package com.vighnesh.service;

import config.RiskConfiguration;
import model.RiskFinding;
import model.RiskReport;
import model.RiskSeverity;
import model.RiskType;
import model.Transaction;
import org.junit.jupiter.api.Test;
import service.RiskEngine;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiskEngineFactoryTest {

    @Test
    void shouldCreateRiskEngineWithDefaultConfiguration() {
        RiskConfiguration config = new RiskConfiguration();
        RiskEngineFactory factory = new RiskEngineFactory(config);

        RiskEngine engine = factory.create();
        assertNotNull(engine);

        // Transaction with normal non-round amount, within normal time, below high amount, single transaction (no duplicate, velocity, concentration)
        Transaction transaction = new Transaction(
                "TXN100",
                "Normal Vendor",
                "EMP200",
                new BigDecimal("50000.50"),
                LocalDateTime.of(2026, 8, 20, 14, 30),
                "Office Supplies"
        );

        RiskReport report = engine.analyze(transaction, List.of(transaction));
        assertEquals(0, report.getRiskScore());
        assertEquals(RiskSeverity.LOW, report.getRiskLevel());
        assertTrue(report.getFindings().isEmpty());
    }

    @Test
    void shouldCreateRiskEngineWithAllSixRulesActive() {
        RiskConfiguration config = new RiskConfiguration();
        RiskEngineFactory factory = new RiskEngineFactory(config);
        RiskEngine engine = factory.create();

        LocalDateTime baseTime = LocalDateTime.of(2026, 8, 20, 20, 0); // outside 09:00 - 18:00 (UnusualTime: 20)

        // EMP101 makes 3 round-amount 150000 transactions with ABC Suppliers within 20 mins:
        // - HighAmount (>100000) -> 30
        // - UnusualTime (20:00) -> 20
        // - RoundAmount (multiple of 10000) -> 10
        // - Duplicate (within 10 min window of t2) -> 25
        // - TransactionVelocity (3 txns within 30 min) -> 20
        // - VendorConcentration (3/3 = 100% with ABC Suppliers >= 70%) -> 15
        Transaction t1 = new Transaction("TXN001", "ABC Suppliers", "EMP101", new BigDecimal("150000.00"), baseTime, "Supplies");
        Transaction t2 = new Transaction("TXN002", "ABC Suppliers", "EMP101", new BigDecimal("150000.00"), baseTime.plusMinutes(5), "Supplies");
        Transaction t3 = new Transaction("TXN003", "ABC Suppliers", "EMP101", new BigDecimal("150000.00"), baseTime.plusMinutes(15), "Supplies");

        List<Transaction> dataset = List.of(t1, t2, t3);

        RiskReport report = engine.analyze(t1, dataset);

        assertEquals(100, report.getRiskScore());
        assertEquals(RiskSeverity.CRITICAL, report.getRiskLevel());
        assertEquals(6, report.getFindings().size());

        assertTrue(report.getFindings().stream().anyMatch(f -> f.getType() == RiskType.HIGH_AMOUNT));
        assertTrue(report.getFindings().stream().anyMatch(f -> f.getType() == RiskType.UNUSUAL_TRANSACTION_TIME));
        assertTrue(report.getFindings().stream().anyMatch(f -> f.getType() == RiskType.ROUND_AMOUNT));
        assertTrue(report.getFindings().stream().anyMatch(f -> f.getType() == RiskType.DUPLICATE_TRANSACTION));
        assertTrue(report.getFindings().stream().anyMatch(f -> f.getType() == RiskType.TRANSACTION_VELOCITY));
        assertTrue(report.getFindings().stream().anyMatch(f -> f.getType() == RiskType.VENDOR_CONCENTRATION));
    }

    @Test
    void shouldApplyCustomConfigurationToRules() {
        RiskConfiguration config = new RiskConfiguration();
        // Lower threshold to 40,000 with score 50
        config.getHighAmount().setThreshold(new BigDecimal("40000"));
        config.getHighAmount().setScore(50);

        // Narrow business hours to 10:00 - 16:00 with score 30
        config.getUnusualTime().setBusinessStart(LocalTime.of(10, 0));
        config.getUnusualTime().setBusinessEnd(LocalTime.of(16, 0));
        config.getUnusualTime().setScore(30);

        // Duplicate window 30 min with score 40
        config.getDuplicate().setWindowMinutes(30);
        config.getDuplicate().setScore(40);

        // Round amount unit 5000 with score 15
        config.getRoundAmount().setUnit(new BigDecimal("5000"));
        config.getRoundAmount().setScore(15);

        // Transaction velocity window 60 min, threshold 2, score 25
        config.getTransactionVelocity().setWindowMinutes(60);
        config.getTransactionVelocity().setThreshold(2);
        config.getTransactionVelocity().setScore(25);

        // Vendor concentration threshold 60%, min 2, score 20
        config.getVendorConcentration().setThresholdPercent(60.0);
        config.getVendorConcentration().setMinimumTransactions(2);
        config.getVendorConcentration().setScore(20);

        RiskEngineFactory factory = new RiskEngineFactory(config);
        RiskEngine engine = factory.create();

        LocalDateTime baseTime = LocalDateTime.of(2026, 8, 20, 9, 30); // outside 10:00-16:00
        Transaction t1 = new Transaction(
                "TXN001",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("45000.00"), // multiple of 5000, above 40,000 threshold
                baseTime,
                "Office Supplies"
        );

        Transaction t2 = new Transaction(
                "TXN002",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("45000.00"),
                baseTime.plusMinutes(25), // within 30 min duplicate window, within 60 min velocity window
                "Office Supplies"
        );

        RiskReport report = engine.analyze(t1, List.of(t1, t2));

        // HighAmount (50) + UnusualTime (30) + RoundAmount (15) + Duplicate (40) + Velocity (25) + VendorConcentration (20)
        assertEquals(100, report.getRiskScore());
        assertEquals(RiskSeverity.CRITICAL, report.getRiskLevel());
        assertEquals(6, report.getFindings().size());

        assertTrue(report.getFindings().stream().anyMatch(f -> f.getType() == RiskType.HIGH_AMOUNT && f.getScore() == 50));
        assertTrue(report.getFindings().stream().anyMatch(f -> f.getType() == RiskType.UNUSUAL_TRANSACTION_TIME && f.getScore() == 30));
        assertTrue(report.getFindings().stream().anyMatch(f -> f.getType() == RiskType.DUPLICATE_TRANSACTION && f.getScore() == 40));
        assertTrue(report.getFindings().stream().anyMatch(f -> f.getType() == RiskType.ROUND_AMOUNT && f.getScore() == 15));
        assertTrue(report.getFindings().stream().anyMatch(f -> f.getType() == RiskType.TRANSACTION_VELOCITY && f.getScore() == 25));
        assertTrue(report.getFindings().stream().anyMatch(f -> f.getType() == RiskType.VENDOR_CONCENTRATION && f.getScore() == 20));
    }
}
