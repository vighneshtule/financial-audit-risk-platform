package service;

import com.vighnesh.FinancialAuditRiskApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import repository.TransactionRepository;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = FinancialAuditRiskApplication.class)
@Testcontainers
class TransactionImportServiceTest {

    static {
        System.setProperty("user.timezone", "UTC");
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
    }

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("financial_audit")
                    .withUsername("postgres")
                    .withPassword("postgres")
                    .withInitScript("schema.sql");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () -> postgres.getJdbcUrl() + "&options=-c%20TimeZone=UTC"
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
    private DataSource dataSource;

    @Autowired
    private TransactionRepository repository;

    @Test
    void failedImportShouldRollbackAllTransactions() throws Exception {
        TransactionCsvReader csvReader = new TransactionCsvReader();
        TransactionImportService importService =
                new TransactionImportService(csvReader, repository, dataSource);

        int beforeCount = repository.findAll().size();

        assertThrows(
                Exception.class,
                () -> importService.importFromCsv("data/transactions.csv")
        );

        int afterCount = repository.findAll().size();

        assertEquals(
                beforeCount,
                afterCount,
                "Database should remain unchanged after failed import"
        );
    }
}