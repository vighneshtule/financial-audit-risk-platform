package com.vighnesh.controller;

import com.vighnesh.FinancialAuditRiskApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = FinancialAuditRiskApplication.class)
@AutoConfigureMockMvc
@Testcontainers
class RiskEvidenceIntegrationTest {

    static {
        System.setProperty("user.timezone", "UTC");
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
    }

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("financial_audit")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl() + "&options=-c%20TimeZone=UTC");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanAuditHistory() {
        jdbcTemplate.execute("TRUNCATE TABLE risk_findings, risk_analysis_runs RESTART IDENTITY CASCADE");
    }

    @Test
    void getUnknownTransactionContextShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/risk/transactions/UNKNOWN_TXN/context"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void existingTransactionReturnsContextWithUnanalyzedRelatedTransactions() throws Exception {
        // TXN004 has vendor="Global Tech", employee="EMP103", category="Technology"
        // TXN005 shares vendor, employee, and category with TXN004
        mockMvc.perform(get("/api/risk/transactions/TXN004/context"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TXN004"))
                .andExpect(jsonPath("$.vendor.name").value("Global Tech"))
                .andExpect(jsonPath("$.vendor.relatedTransactionCount").value(1))
                .andExpect(jsonPath("$.vendor.transactions[0].transactionId").value("TXN005"))
                .andExpect(jsonPath("$.vendor.transactions[0].riskScore").value(nullValue()))
                .andExpect(jsonPath("$.vendor.transactions[0].riskLevel").value(nullValue()))
                .andExpect(jsonPath("$.employee.name").value("EMP103"))
                .andExpect(jsonPath("$.employee.relatedTransactionCount").value(1))
                .andExpect(jsonPath("$.employee.transactions[0].transactionId").value("TXN005"))
                .andExpect(jsonPath("$.category.name").value("Technology"))
                .andExpect(jsonPath("$.category.relatedTransactionCount").value(1))
                .andExpect(jsonPath("$.category.transactions[0].transactionId").value("TXN005"))
                .andExpect(jsonPath("$.evidenceSummary.relatedTransactionCount").value(1));
    }

    @Test
    void contextExcludesCurrentTransactionAndCalculatesAggregatesCorrectly() throws Exception {
        // First analyze TXN005 so it has persisted risk information
        mockMvc.perform(post("/api/risk/analyze/TXN005"))
                .andExpect(status().isOk());

        // Now query TXN004 context
        mockMvc.perform(get("/api/risk/transactions/TXN004/context"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TXN004"))
                // TXN004 should NOT be in the related transactions list
                .andExpect(jsonPath("$.vendor.transactions[*].transactionId", not(hasItem("TXN004"))))
                .andExpect(jsonPath("$.employee.transactions[*].transactionId", not(hasItem("TXN004"))))
                .andExpect(jsonPath("$.category.transactions[*].transactionId", not(hasItem("TXN004"))))

                // Verify vendor group aggregates
                .andExpect(jsonPath("$.vendor.relatedTransactionCount").value(1))
                .andExpect(jsonPath("$.vendor.totalAmount").value(150000.00))
                .andExpect(jsonPath("$.vendor.highestRiskScore").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.vendor.transactions[0].transactionId").value("TXN005"))
                .andExpect(jsonPath("$.vendor.transactions[0].riskScore").isNumber())
                .andExpect(jsonPath("$.vendor.transactions[0].riskLevel").isString())

                // Verify evidence summary
                .andExpect(jsonPath("$.evidenceSummary.relatedTransactionCount").value(1))
                .andExpect(jsonPath("$.evidenceSummary.relatedAmount").value(150000.00));
    }

    @Test
    void multiRelatedTransactionAggregatesAndFlaggedCounts() throws Exception {
        // TXN001, TXN002, TXN007 all share vendor "ABC Suppliers" and employee "EMP101"
        // Analyze TXN002 and TXN007
        mockMvc.perform(post("/api/risk/analyze/TXN002")).andExpect(status().isOk());
        mockMvc.perform(post("/api/risk/analyze/TXN007")).andExpect(status().isOk());

        // Query context for TXN001
        mockMvc.perform(get("/api/risk/transactions/TXN001/context"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TXN001"))
                .andExpect(jsonPath("$.vendor.name").value("ABC Suppliers"))
                .andExpect(jsonPath("$.vendor.relatedTransactionCount").value(2))
                .andExpect(jsonPath("$.vendor.transactions", hasSize(2)))
                .andExpect(jsonPath("$.vendor.transactions[*].transactionId", not(hasItem("TXN001"))))
                .andExpect(jsonPath("$.employee.relatedTransactionCount").value(2))
                .andExpect(jsonPath("$.employee.transactions", hasSize(2)))
                .andExpect(jsonPath("$.evidenceSummary.relatedTransactionCount").value(greaterThanOrEqualTo(2)));
    }
}
