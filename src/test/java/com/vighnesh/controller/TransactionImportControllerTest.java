package com.vighnesh.controller;

import com.vighnesh.FinancialAuditRiskApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        classes = FinancialAuditRiskApplication.class
)
@AutoConfigureMockMvc
@Testcontainers
class TransactionImportControllerTest {

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
    private JdbcTemplate jdbcTemplate;

    /** Valid 3-row CSV content (header + 3 data rows). */
    private static final String VALID_CSV =
            "transaction_id,vendor,employee,amount,transaction_time,category\n" +
            "IMP001,Test Vendor A,EMP901,50000,2026-09-01T09:00,Office\n" +
            "IMP002,Test Vendor B,EMP902,120000,2026-09-01T10:00,Technology\n" +
            "IMP003,Test Vendor C,EMP903,30000,2026-09-01T23:00,Equipment\n";

    @BeforeEach
    void cleanTransactions() {
        // Remove only the import-test rows so existing seed data
        // from data.sql (TXN001–TXN010) is untouched per test.
        jdbcTemplate.execute(
                "DELETE FROM transactions WHERE transaction_id LIKE 'IMP%'"
        );
    }

    // -------------------------------------------------------
    // 1. Successful CSV upload
    // -------------------------------------------------------
    @Test
    void successfulCsvImportReturns200WithCount()
            throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                MediaType.TEXT_PLAIN_VALUE,
                VALID_CSV.getBytes()
        );

        mockMvc.perform(multipart("/api/transactions/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.message").value(
                        "Transactions imported successfully"
                ));
    }

    // -------------------------------------------------------
    // 2. Missing / empty file
    // -------------------------------------------------------
    @Test
    void emptyFileShouldReturn400()
            throws Exception {

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.csv",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[0]
        );

        mockMvc.perform(multipart("/api/transactions/import").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // -------------------------------------------------------
    // 3. Invalid file extension
    // -------------------------------------------------------
    @Test
    void nonCsvFileShouldReturn400()
            throws Exception {

        MockMultipartFile txtFile = new MockMultipartFile(
                "file",
                "data.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "some text content".getBytes()
        );

        mockMvc.perform(multipart("/api/transactions/import").file(txtFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "Only CSV files are accepted (.csv extension required)"
                ));
    }

    // -------------------------------------------------------
    // 4. Malformed CSV (header-only, no data rows)
    // -------------------------------------------------------
    @Test
    void csvWithOnlyHeaderShouldReturn400()
            throws Exception {

        MockMultipartFile headerOnlyFile = new MockMultipartFile(
                "file",
                "header_only.csv",
                MediaType.TEXT_PLAIN_VALUE,
                "transaction_id,vendor,employee,amount,transaction_time,category\n"
                        .getBytes()
        );

        // The existing TransactionCsvReader reads all lines and
        // skips index 0 (header). With only 1 line, the for-loop
        // body never executes -> importFromCsv returns 0.
        // A 0-row import is valid (no DB error). Backend returns 200
        // with count=0.
        mockMvc.perform(
                multipart("/api/transactions/import").file(headerOnlyFile)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    // -------------------------------------------------------
    // 5. Malformed CSV data row (bad number format)
    // -------------------------------------------------------
    @Test
    void malformedCsvDataRowShouldReturn400()
            throws Exception {

        String badCsv =
                "transaction_id,vendor,employee,amount,transaction_time,category\n" +
                "IMP_BAD,Bad Vendor,EMP999,NOT_A_NUMBER,2026-09-01T09:00,Office\n";

        MockMultipartFile badFile = new MockMultipartFile(
                "file",
                "malformed.csv",
                MediaType.TEXT_PLAIN_VALUE,
                badCsv.getBytes()
        );

        mockMvc.perform(multipart("/api/transactions/import").file(badFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
