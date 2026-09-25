package com.vighnesh.service;

import config.RiskConfiguration;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.RiskAnalysisRunRepository;
import repository.RiskFindingRepository;
import repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskAnalysisServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RiskAnalysisRunRepository riskAnalysisRunRepository;

    @Mock
    private RiskFindingRepository riskFindingRepository;

    private RiskEngineFactory riskEngineFactory;
    private RiskAnalysisService service;

    @BeforeEach
    void setUp() {
        RiskConfiguration config = new RiskConfiguration();
        riskEngineFactory = new RiskEngineFactory(config);
        service = new RiskAnalysisService(
                transactionRepository,
                riskAnalysisRunRepository,
                riskFindingRepository,
                riskEngineFactory
        );
    }

    @Test
    void analyzeAllWithEmptyDatabaseShouldReturnZeroCounts() throws Exception {
        when(transactionRepository.findAll()).thenReturn(Collections.emptyList());

        AnalysisResult result = service.analyzeAndPersistAllTransactions();

        assertEquals(0, result.getTransactionsAnalyzed());
        assertEquals(0, result.getLowRisk());
        assertEquals(0, result.getMediumRisk());
        assertEquals(0, result.getHighRisk());
        assertEquals(0, result.getCriticalRisk());
        assertEquals(0, result.getHighestRiskScore());

        verify(riskAnalysisRunRepository, never()).save(anyString(), anyInt(), any());
        verify(riskFindingRepository, never()).save(anyLong(), anyString(), any());
    }

    @Test
    void analyzeAllWithMultipleTransactionsShouldPersistRunsAndFindings() throws Exception {
        LocalDateTime baseTime = LocalDateTime.of(2026, 8, 20, 14, 0);

        // Low risk: standard transaction
        Transaction lowTxn = new Transaction(
                "TXN001",
                "Vendor A",
                "EMP01",
                new BigDecimal("500.00"),
                baseTime,
                "Office"
        );

        // High risk: high amount (>100000: 30) + unusual time (22:00: 20) + round amount (10) = 60 (HIGH)
        Transaction highTxn = new Transaction(
                "TXN002",
                "Vendor B",
                "EMP02",
                new BigDecimal("150000.00"),
                LocalDateTime.of(2026, 8, 20, 22, 0),
                "Services"
        );

        when(transactionRepository.findAll()).thenReturn(List.of(lowTxn, highTxn));
        when(riskAnalysisRunRepository.save(eq("TXN001"), anyInt(), any(RiskSeverity.class))).thenReturn(101L);
        when(riskAnalysisRunRepository.save(eq("TXN002"), anyInt(), any(RiskSeverity.class))).thenReturn(102L);

        AnalysisResult result = service.analyzeAndPersistAllTransactions();

        assertEquals(2, result.getTransactionsAnalyzed());
        assertEquals(1, result.getLowRisk());
        assertEquals(0, result.getMediumRisk());
        assertEquals(1, result.getHighRisk());
        assertEquals(0, result.getCriticalRisk());
        assertEquals(60, result.getHighestRiskScore());

        // Verify runs persisted
        verify(riskAnalysisRunRepository).save(eq("TXN001"), eq(0), eq(RiskSeverity.LOW));
        verify(riskAnalysisRunRepository).save(eq("TXN002"), eq(60), eq(RiskSeverity.HIGH));

        // Verify findings persisted for TXN002 (3 findings: High amount, Unusual time, Round amount)
        verify(riskFindingRepository, times(3)).save(eq(102L), eq("TXN002"), any(RiskFinding.class));
    }

    @Test
    void analyzeAllPropagatesExceptionWhenPersistenceFails() throws Exception {
        Transaction txn = new Transaction(
                "TXN001",
                "Vendor A",
                "EMP01",
                new BigDecimal("500.00"),
                LocalDateTime.of(2026, 8, 20, 14, 0),
                "Office"
        );

        when(transactionRepository.findAll()).thenReturn(List.of(txn));
        when(riskAnalysisRunRepository.save(anyString(), anyInt(), any()))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> service.analyzeAndPersistAllTransactions());
    }
}
