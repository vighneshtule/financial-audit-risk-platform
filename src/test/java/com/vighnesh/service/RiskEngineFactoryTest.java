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

        // Transaction with normal amount, time, and no duplicates
        Transaction transaction = new Transaction(
                "TXN100",
                "Normal Vendor",
                "EMP200",
                new BigDecimal("50000.00"),
                LocalDateTime.of(2026, 8, 20, 14, 30),
                "Office Supplies"
        );

        RiskReport report = engine.analyze(transaction, List.of(transaction));
        assertEquals(0, report.getRiskScore());
        assertEquals(RiskSeverity.LOW, report.getRiskLevel());
        assertTrue(report.getFindings().isEmpty());
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

        RiskEngineFactory factory = new RiskEngineFactory(config);
        RiskEngine engine = factory.create();

        LocalDateTime baseTime = LocalDateTime.of(2026, 8, 20, 9, 30); // outside 10:00-16:00
        Transaction t1 = new Transaction(
                "TXN001",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("45000.00"), // above 40,000 threshold
                baseTime,
                "Office Supplies"
        );

        Transaction t2 = new Transaction(
                "TXN002",
                "ABC Suppliers",
                "EMP101",
                new BigDecimal("45000.00"),
                baseTime.plusMinutes(25), // within 30 min duplicate window
                "Office Supplies"
        );

        RiskReport report = engine.analyze(t1, List.of(t1, t2));

        // Expect high amount (50) + unusual time (30) + duplicate (40) = 120 -> capped at 100
        assertEquals(100, report.getRiskScore());
        assertEquals(RiskSeverity.CRITICAL, report.getRiskLevel());
        assertEquals(3, report.getFindings().size());

        assertTrue(report.getFindings().stream().anyMatch(f -> f.getType() == RiskType.HIGH_AMOUNT && f.getScore() == 50));
        assertTrue(report.getFindings().stream().anyMatch(f -> f.getType() == RiskType.UNUSUAL_TRANSACTION_TIME && f.getScore() == 30));
        assertTrue(report.getFindings().stream().anyMatch(f -> f.getType() == RiskType.DUPLICATE_TRANSACTION && f.getScore() == 40));
    }
}
