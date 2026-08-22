package com.vighnesh.controller;

import com.vighnesh.FinancialAuditRiskApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
