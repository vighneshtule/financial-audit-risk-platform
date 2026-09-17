package com.vighnesh.service;

import model.RiskFinding;
import model.RiskSeverity;
import model.RiskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RiskScoreCalculatorTest {

    private RiskScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new RiskScoreCalculator();
    }

    // A. Empty findings: score = 0, severity = LOW
    @Test
    void shouldReturnZeroScoreAndLowSeverityForEmptyFindings() {
        int score = calculator.calculateScore(Collections.emptyList());
        assertEquals(0, score);

        RiskSeverity severity = calculator.determineSeverity(score);
        assertEquals(RiskSeverity.LOW, severity);
    }

    @Test
    void shouldReturnZeroScoreForNullFindings() {
        int score = calculator.calculateScore(null);
        assertEquals(0, score);

        RiskSeverity severity = calculator.determineSeverity(score);
        assertEquals(RiskSeverity.LOW, severity);
    }

    // B. One finding: verify its score is returned
    @Test
    void shouldReturnScoreOfSingleFinding() {
        RiskFinding finding = new RiskFinding(
                RiskType.HIGH_AMOUNT,
                30,
                RiskSeverity.MEDIUM,
                "Amount exceeds threshold"
        );

        int score = calculator.calculateScore(List.of(finding));
        assertEquals(30, score);
    }

    // C. Multiple findings: verify scores are summed
    @Test
    void shouldSumScoresOfMultipleFindings() {
        RiskFinding f1 = new RiskFinding(
                RiskType.HIGH_AMOUNT,
                30,
                RiskSeverity.MEDIUM,
                "Amount exceeds threshold"
        );
        RiskFinding f2 = new RiskFinding(
                RiskType.UNUSUAL_TRANSACTION_TIME,
                20,
                RiskSeverity.LOW,
                "Transaction outside business hours"
        );
        RiskFinding f3 = new RiskFinding(
                RiskType.DUPLICATE_TRANSACTION,
                25,
                RiskSeverity.LOW,
                "Duplicate transaction detected"
        );

        int score = calculator.calculateScore(List.of(f1, f2, f3));
        assertEquals(75, score);
    }

    // D. Score cap: findings whose total exceeds 100 capped at exactly 100
    @Test
    void shouldCapTotalScoreAt100WhenExceeding100() {
        RiskFinding f1 = new RiskFinding(
                RiskType.HIGH_AMOUNT,
                60,
                RiskSeverity.HIGH,
                "High amount"
        );
        RiskFinding f2 = new RiskFinding(
                RiskType.DUPLICATE_TRANSACTION,
                55,
                RiskSeverity.MEDIUM,
                "Duplicate"
        );

        int score = calculator.calculateScore(List.of(f1, f2));
        assertEquals(100, score);
    }

    @Test
    void shouldHandleExact100ScoreWithoutExceeding() {
        RiskFinding f1 = new RiskFinding(
                RiskType.HIGH_AMOUNT,
                50,
                RiskSeverity.MEDIUM,
                "High amount"
        );
        RiskFinding f2 = new RiskFinding(
                RiskType.UNUSUAL_TRANSACTION_TIME,
                50,
                RiskSeverity.MEDIUM,
                "Unusual time"
        );

        int score = calculator.calculateScore(List.of(f1, f2));
        assertEquals(100, score);
    }

    // E. Severity boundaries:
    // 29 -> LOW
    // 30 -> MEDIUM
    // 59 -> MEDIUM
    // 60 -> HIGH
    // 79 -> HIGH
    // 80 -> CRITICAL
    // 100 -> CRITICAL
    @Test
    void shouldReturnLowForScoreBelow30() {
        assertEquals(RiskSeverity.LOW, calculator.determineSeverity(0));
        assertEquals(RiskSeverity.LOW, calculator.determineSeverity(29));
    }

    @Test
    void shouldReturnMediumForScoreBetween30And59() {
        assertEquals(RiskSeverity.MEDIUM, calculator.determineSeverity(30));
        assertEquals(RiskSeverity.MEDIUM, calculator.determineSeverity(59));
    }

    @Test
    void shouldReturnHighForScoreBetween60And79() {
        assertEquals(RiskSeverity.HIGH, calculator.determineSeverity(60));
        assertEquals(RiskSeverity.HIGH, calculator.determineSeverity(79));
    }

    @Test
    void shouldReturnCriticalForScoreBetween80And100() {
        assertEquals(RiskSeverity.CRITICAL, calculator.determineSeverity(80));
        assertEquals(RiskSeverity.CRITICAL, calculator.determineSeverity(100));
    }
}
