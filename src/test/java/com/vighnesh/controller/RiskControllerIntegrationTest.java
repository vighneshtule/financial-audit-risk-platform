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

import repository.RiskFindingRepository;
import repository.RiskAnalysisRunRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        classes = FinancialAuditRiskApplication.class
)
@AutoConfigureMockMvc
@Testcontainers
class RiskControllerIntegrationTest {

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
    static void configureDatabase(
            DynamicPropertyRegistry registry) {

        registry.add(
                "spring.datasource.url",
                () -> postgres.getJdbcUrl()
                        + "&options=-c%20TimeZone=UTC"
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RiskFindingRepository riskFindingRepository;

    @Autowired
    private RiskAnalysisRunRepository riskAnalysisRunRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanAuditHistory() {

        jdbcTemplate.execute(
                "TRUNCATE TABLE risk_findings, risk_analysis_runs RESTART IDENTITY CASCADE"
        );
    }

    @Test
    void contextLoads() {
        // Verifies that the complete Spring application
        // starts successfully with PostgreSQL.
    }

    @Test
    void getUnknownTransactionShouldReturn404()
            throws Exception {

        mockMvc.perform(
                get("/api/risk/transactions/DOES_NOT_EXIST")
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(
                jsonPath("$.error").value("Not Found")
        );
    }

    @Test
    void getExistingTransactionShouldReturnRiskReport()
            throws Exception {

        mockMvc.perform(
                get("/api/risk/transactions/TXN008")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.riskScore").value(50))
        .andExpect(jsonPath("$.riskLevel").value("MEDIUM"))
        .andExpect(jsonPath("$.findings").isArray())
        .andExpect(jsonPath("$.findings.length()").value(2));
    }

    @Test
    void getRiskSummaryShouldReturnCorrectSummary()
            throws Exception {

        mockMvc.perform(
                get("/api/risk/summary")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalTransactions").value(10))
        .andExpect(jsonPath("$.totalAmount").value(790000.00))
        .andExpect(jsonPath("$.lowRiskTransactions").value(7))
        .andExpect(jsonPath("$.mediumRiskTransactions").value(3))
        .andExpect(jsonPath("$.highRiskTransactions").value(0))
        .andExpect(jsonPath("$.criticalRiskTransactions").value(0))
        .andExpect(jsonPath("$.totalFindings").value(10))
        .andExpect(jsonPath("$.highestRiskScore").value(55));
    }

    @Test
    void getAllRiskTransactionsShouldReturnRiskAnalysis()
            throws Exception {

        mockMvc.perform(
                get("/api/risk/transactions")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(10))
        .andExpect(jsonPath("$.content[0].transactionId").exists())
        .andExpect(jsonPath("$.content[0].riskScore").exists())
        .andExpect(jsonPath("$.content[0].riskLevel").exists())
        .andExpect(jsonPath("$.content[0].findings").exists())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.totalElements").value(10))
        .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getRiskTransactionsShouldIncludeTxn008()
            throws Exception {

        String response =
                mockMvc.perform(
                        get("/api/risk/transactions")
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(response.contains("\"transactionId\":\"TXN008\""));
        assertTrue(response.contains("\"riskScore\":50"));
    }

    @Test
    void filterTransactionsByMediumRisk()
            throws Exception {

        mockMvc.perform(
                get("/api/risk/transactions")
                        .param("riskLevel", "MEDIUM")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(3))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void filterTransactionsByLowRisk()
            throws Exception {

        mockMvc.perform(
                get("/api/risk/transactions")
                        .param("riskLevel", "LOW")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(7))
        .andExpect(jsonPath("$.totalElements").value(7))
        .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void filterTransactionsByMinimumRiskScore()
                throws Exception {

        mockMvc.perform(
                get("/api/risk/transactions")
                        .param("minScore", "50")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(3))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void filterTransactionsByMinimumRiskScore55()
                throws Exception {

        mockMvc.perform(
                get("/api/risk/transactions")
                        .param("minScore", "55")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void filterTransactionsByRiskLevelAndMinimumScore()
            throws Exception {

        mockMvc.perform(
                get("/api/risk/transactions")
                        .param("riskLevel", "MEDIUM")
                        .param("minScore", "50")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(3))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(1));
    }

        @Test
        void paginationShouldReturnFirstFiveTransactions()
                throws Exception {

        mockMvc.perform(
                get("/api/risk/transactions")
                        .param("page", "0")
                        .param("size", "5")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(5))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(5))
        .andExpect(jsonPath("$.totalElements").value(10))
        .andExpect(jsonPath("$.totalPages").value(2));
        }

        @Test
        void paginationShouldReturnSecondPage()
                throws Exception {

        mockMvc.perform(
                get("/api/risk/transactions")
                        .param("page", "1")
                        .param("size", "5")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(5))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.size").value(5))
        .andExpect(jsonPath("$.totalElements").value(10))
        .andExpect(jsonPath("$.totalPages").value(2));
        }

        @Test
        void paginationShouldWorkWithRiskFilter()
                throws Exception {

        mockMvc.perform(
                get("/api/risk/transactions")
                        .param("riskLevel", "MEDIUM")
                        .param("page", "0")
                        .param("size", "2")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(2));
        }

    @Test
    void analyzeTransactionShouldPersistFindings()
            throws Exception {

        mockMvc.perform(
                post("/api/risk/analyze/TXN008")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.riskScore").value(50))
        .andExpect(jsonPath("$.riskLevel").value("MEDIUM"))
        .andExpect(jsonPath("$.findings.length()").value(2));
    }

    @Test
    void repeatedAnalysisShouldNotDuplicateFindings()
            throws Exception {

        mockMvc.perform(
                post("/api/risk/analyze/TXN008")
        )
        .andExpect(status().isOk());

        mockMvc.perform(
                post("/api/risk/analyze/TXN008")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.findings.length()").value(2));
    }

    @Test
    void analyzeUnknownTransactionShouldReturn404()
            throws Exception {

        mockMvc.perform(
                post("/api/risk/analyze/DOES_NOT_EXIST")
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(
                jsonPath("$.error").value("Not Found")
        );
    }

    @Test
    void repeatedAnalysisShouldPersistExactlyOneSetOfFindings()
            throws Exception {

        mockMvc.perform(
                post("/api/risk/analyze/TXN008")
        )
        .andExpect(status().isOk());

        int firstCount =
                riskFindingRepository.countByTransactionId(
                        "TXN008"
                );

        assertEquals(2, firstCount);

        mockMvc.perform(
                post("/api/risk/analyze/TXN008")
        )
        .andExpect(status().isOk());

        int secondCount =
                riskFindingRepository.countByTransactionId(
                        "TXN008"
                );

        assertEquals(4, secondCount);
    }

    @Test
    void getPersistedFindingsShouldReturnStoredFindings()
            throws Exception {

        mockMvc.perform(
                post("/api/risk/analyze/TXN008")
        )
        .andExpect(status().isOk());

        mockMvc.perform(
                get("/api/risk/transactions/TXN008/findings")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].type").exists())
        .andExpect(jsonPath("$[0].score").exists())
        .andExpect(jsonPath("$[0].severity").exists())
        .andExpect(jsonPath("$[0].explanation").exists());
    }

    @Test
    void getPersistedFindingsShouldReturnCorrectRiskData()
            throws Exception {

        mockMvc.perform(
                post("/api/risk/analyze/TXN008")
        )
        .andExpect(status().isOk());

        mockMvc.perform(
                get("/api/risk/transactions/TXN008/findings")
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$[?(@.type == 'HIGH_AMOUNT')]")
                        .exists()
        )
        .andExpect(
                jsonPath("$[?(@.type == 'UNUSUAL_TRANSACTION_TIME')]")
                        .exists()
        );
    }

    @Test
    void getPersistedFindingsForUnknownTransactionShouldReturn404()
            throws Exception {

        mockMvc.perform(
                get("/api/risk/transactions/DOES_NOT_EXIST/findings")
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(
                jsonPath("$.error").value("Not Found")
        );
    }

    @Test
    void repeatedAnalysisShouldCreateSeparateAnalysisRuns()
            throws Exception {

        mockMvc.perform(
                post("/api/risk/analyze/TXN008")
        )
        .andExpect(status().isOk());

        mockMvc.perform(
                post("/api/risk/analyze/TXN008")
        )
        .andExpect(status().isOk());

        int runCount =
                riskAnalysisRunRepository
                        .countByTransactionId("TXN008");

        assertEquals(2, runCount);
    }

    @Test
    void getTransactionRiskHistoryShouldReturnAnalysisRuns()
            throws Exception {

        mockMvc.perform(
                post("/api/risk/analyze/TXN008")
        )
        .andExpect(status().isOk());

        mockMvc.perform(
                get("/api/risk/transactions/TXN008/history")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transactionId").value("TXN008"))
        .andExpect(jsonPath("$.analysisRuns").isArray())
        .andExpect(jsonPath("$.analysisRuns.length()").value(1))
        .andExpect(jsonPath("$.analysisRuns[0].riskScore").value(50))
        .andExpect(jsonPath("$.analysisRuns[0].riskLevel").value("MEDIUM"))
        .andExpect(jsonPath("$.analysisRuns[0].findings").isArray())
        .andExpect(jsonPath("$.analysisRuns[0].findings.length()").value(2));
    }

    @Test
    void getTransactionRiskHistoryShouldPreserveMultipleRuns()
            throws Exception {

        mockMvc.perform(
                post("/api/risk/analyze/TXN008")
        )
        .andExpect(status().isOk());

        mockMvc.perform(
                post("/api/risk/analyze/TXN008")
        )
        .andExpect(status().isOk());

        mockMvc.perform(
                get("/api/risk/transactions/TXN008/history")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transactionId").value("TXN008"))
        .andExpect(jsonPath("$.analysisRuns").isArray())
        .andExpect(jsonPath("$.analysisRuns.length()").value(2))
        .andExpect(jsonPath("$.analysisRuns[0].findings").isArray())
        .andExpect(jsonPath("$.analysisRuns[0].findings.length()").value(2))
        .andExpect(jsonPath("$.analysisRuns[1].findings").isArray())
        .andExpect(jsonPath("$.analysisRuns[1].findings.length()").value(2));
    }

    @Test
    void getUnknownTransactionHistoryShouldReturn404()
            throws Exception {

        mockMvc.perform(
                get("/api/risk/transactions/DOES_NOT_EXIST/history")
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"));
    }
}

