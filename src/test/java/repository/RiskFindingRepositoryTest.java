package repository;

import com.vighnesh.FinancialAuditRiskApplication;
import model.RiskFinding;
import model.RiskSeverity;
import model.RiskType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import repository.RiskAnalysisRunRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        classes = FinancialAuditRiskApplication.class
)
@Testcontainers
class RiskFindingRepositoryTest {

    static {
        System.setProperty("user.timezone", "UTC");
        java.util.TimeZone.setDefault(
                java.util.TimeZone.getTimeZone("UTC")
        );
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
    private RiskFindingRepository riskFindingRepository;

    @Autowired
    private RiskAnalysisRunRepository riskAnalysisRunRepository;

    @Test
    void repositoryShouldSaveAndFindRiskFinding()
            throws Exception {

        long analysisRunId =
                riskAnalysisRunRepository.save(
                        "TXN001",
                        30,
                        RiskSeverity.MEDIUM
                );

        RiskFinding finding =
                new RiskFinding(
                        RiskType.HIGH_AMOUNT,
                        30,
                        RiskSeverity.MEDIUM,
                        "Transaction amount exceeds threshold"
                );

        riskFindingRepository.save(
                analysisRunId,
                "TXN001",
                finding
        );

        List<RiskFinding> findings =
                riskFindingRepository.findByAnalysisRunId(
                        analysisRunId
                );

        assertEquals(1, findings.size());

        RiskFinding saved = findings.get(0);

        assertEquals(
                RiskType.HIGH_AMOUNT,
                saved.getType()
        );

        assertEquals(
                30,
                saved.getScore()
        );

        assertEquals(
                RiskSeverity.MEDIUM,
                saved.getSeverity()
        );

        assertEquals(
                "Transaction amount exceeds threshold",
                saved.getExplanation()
        );
    }

    @Test
    void repositoryShouldFindAllFindingsForAnalysisRun()
            throws Exception {

        long analysisRunId =
                riskAnalysisRunRepository.save(
                        "TXN002",
                        55,
                        RiskSeverity.MEDIUM
                );

        RiskFinding first =
                new RiskFinding(
                        RiskType.HIGH_AMOUNT,
                        30,
                        RiskSeverity.MEDIUM,
                        "High amount"
                );

        RiskFinding second =
                new RiskFinding(
                        RiskType.DUPLICATE_TRANSACTION,
                        25,
                        RiskSeverity.MEDIUM,
                        "Duplicate transaction"
                );

        riskFindingRepository.save(
                analysisRunId,
                "TXN002",
                first
        );

        riskFindingRepository.save(
                analysisRunId,
                "TXN002",
                second
        );

        List<RiskFinding> findings =
                riskFindingRepository.findByAnalysisRunId(
                        analysisRunId
                );

        assertEquals(2, findings.size());
    }
}