package com.vighnesh.service;

import com.vighnesh.FinancialAuditRiskApplication;
import com.vighnesh.exception.AnalysisRunNotFoundException;
import com.vighnesh.exception.TransactionNotFoundException;
import model.AuditDecision;
import model.AuditDecisionType;
import model.AuditEvent;
import model.CreateAuditDecisionRequest;
import model.RiskAnalysisRun;
import model.RiskFinding;
import model.RiskReport;
import model.RiskSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import repository.AuditDecisionRepository;
import repository.AuditEventRepository;
import repository.RiskAnalysisRunRepository;
import repository.RiskFindingRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = FinancialAuditRiskApplication.class)
class AuditServiceTest {

    static {
        System.setProperty("user.timezone", "UTC");
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
    }

    static PostgreSQLContainer<?> postgres;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        try {
            if (org.testcontainers.DockerClientFactory.instance().isDockerAvailable()) {
                postgres = new PostgreSQLContainer<>("postgres:16")
                        .withDatabaseName("financial_audit")
                        .withUsername("postgres")
                        .withPassword("postgres");
                postgres.start();
                registry.add("spring.datasource.url", () -> postgres.getJdbcUrl() + "&options=-c%20TimeZone=UTC");
                registry.add("spring.datasource.username", postgres::getUsername);
                registry.add("spring.datasource.password", postgres::getPassword);
                return;
            }
        } catch (Throwable ignored) {
        }
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:auditservdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
    }

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditDecisionRepository auditDecisionRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private RiskAnalysisRunRepository riskAnalysisRunRepository;

    @Autowired
    private RiskFindingRepository riskFindingRepository;

    @Autowired
    private RiskAnalysisService riskAnalysisService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long sampleAnalysisRunId;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.execute("DELETE FROM audit_events");
        jdbcTemplate.execute("DELETE FROM audit_decisions");
        jdbcTemplate.execute("DELETE FROM risk_findings WHERE transaction_id = 'TXN001'");
        jdbcTemplate.execute("DELETE FROM risk_analysis_runs WHERE transaction_id = 'TXN001'");

        sampleAnalysisRunId = riskAnalysisRunRepository.save("TXN001", 30, RiskSeverity.MEDIUM);
    }

    @Test
    void testCreateConfirmedRiskDecisionSuccessfully() throws Exception {
        CreateAuditDecisionRequest req = new CreateAuditDecisionRequest(
                sampleAnalysisRunId,
                AuditDecisionType.CONFIRMED_RISK,
                "Confirmed vendor kickback suspicion",
                "IgnoredClientActor"
        );

        AuditDecision decision = auditService.recordDecision("TXN001", req, "SeniorAuditor");
        assertNotNull(decision);
        assertTrue(decision.getId() > 0);
        assertEquals("TXN001", decision.getTransactionId());
        assertEquals(sampleAnalysisRunId, decision.getAnalysisRunId());
        assertEquals(AuditDecisionType.CONFIRMED_RISK, decision.getDecision());
        assertEquals("Confirmed vendor kickback suspicion", decision.getComment());
        assertEquals("SeniorAuditor", decision.getDecidedBy());
        assertNotNull(decision.getDecidedAt());
    }

    @Test
    void testCreateFalsePositiveDecisionSuccessfully() throws Exception {
        CreateAuditDecisionRequest req = new CreateAuditDecisionRequest(
                sampleAnalysisRunId,
                AuditDecisionType.FALSE_POSITIVE,
                "Legitimate quarterly bulk purchase",
                "IgnoredClientActor"
        );

        AuditDecision decision = auditService.recordDecision("TXN001", req, "ComplianceOfficer");
        assertNotNull(decision);
        assertEquals(AuditDecisionType.FALSE_POSITIVE, decision.getDecision());
        assertEquals("ComplianceOfficer", decision.getDecidedBy());
    }

    @Test
    void testCreateRequiresInvestigationDecisionSuccessfully() throws Exception {
        CreateAuditDecisionRequest req = new CreateAuditDecisionRequest(
                sampleAnalysisRunId,
                AuditDecisionType.REQUIRES_INVESTIGATION,
                "Needs invoice verification",
                "IgnoredClientActor"
        );

        AuditDecision decision = auditService.recordDecision("TXN001", req, "InternalAuditor");
        assertNotNull(decision);
        assertEquals(AuditDecisionType.REQUIRES_INVESTIGATION, decision.getDecision());
        assertEquals("InternalAuditor", decision.getDecidedBy());
    }

    @Test
    void testCreateEscalatedDecisionSuccessfully() throws Exception {
        CreateAuditDecisionRequest req = new CreateAuditDecisionRequest(
                sampleAnalysisRunId,
                AuditDecisionType.ESCALATED,
                "Escalating to Legal and CFO",
                "IgnoredClientActor"
        );

        AuditDecision decision = auditService.recordDecision("TXN001", req, "AuditLead");
        assertNotNull(decision);
        assertEquals(AuditDecisionType.ESCALATED, decision.getDecision());
        assertEquals("AuditLead", decision.getDecidedBy());
    }

    @Test
    void testRejectMissingDecision() {
        CreateAuditDecisionRequest req = new CreateAuditDecisionRequest(
                sampleAnalysisRunId,
                null,
                "Comment without decision",
                "Auditor"
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> auditService.recordDecision("TXN001", req, "Auditor")
        );
        assertTrue(ex.getMessage().contains("Decision is required"));
    }

    @Test
    void testRejectMissingActor() {
        CreateAuditDecisionRequest req = new CreateAuditDecisionRequest(
                sampleAnalysisRunId,
                AuditDecisionType.CONFIRMED_RISK,
                "Comment without actor",
                "Auditor"
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> auditService.recordDecision("TXN001", req, "   ")
        );
        assertTrue(ex.getMessage().contains("Authenticated actor is required"));
    }

    @Test
    void testRejectUnknownTransaction() {
        CreateAuditDecisionRequest req = new CreateAuditDecisionRequest(
                sampleAnalysisRunId,
                AuditDecisionType.CONFIRMED_RISK,
                "Testing unknown txn",
                "Auditor"
        );

        assertThrows(
                TransactionNotFoundException.class,
                () -> auditService.recordDecision("NON_EXISTENT_TXN", req, "Auditor")
        );
    }

    @Test
    void testRejectAnalysisRunIdThatBelongsToAnotherTransaction() throws Exception {
        // Create an analysis run for TXN002
        long txn002RunId = riskAnalysisRunRepository.save("TXN002", 50, RiskSeverity.HIGH);

        CreateAuditDecisionRequest req = new CreateAuditDecisionRequest(
                txn002RunId, // belongs to TXN002, not TXN001
                AuditDecisionType.CONFIRMED_RISK,
                "Mismatched run ID",
                "Auditor"
        );

        assertThrows(
                AnalysisRunNotFoundException.class,
                () -> auditService.recordDecision("TXN001", req, "Auditor")
        );
    }

    @Test
    void testCreatingTwoDecisionsPreservesBothRecords() throws Exception {
        CreateAuditDecisionRequest req1 = new CreateAuditDecisionRequest(
                sampleAnalysisRunId,
                AuditDecisionType.REQUIRES_INVESTIGATION,
                "Initial triage",
                "Ignored"
        );
        AuditDecision d1 = auditService.recordDecision("TXN001", req1, "Auditor1");

        CreateAuditDecisionRequest req2 = new CreateAuditDecisionRequest(
                sampleAnalysisRunId,
                AuditDecisionType.CONFIRMED_RISK,
                "Follow up confirmed anomaly",
                "Ignored"
        );
        AuditDecision d2 = auditService.recordDecision("TXN001", req2, "Auditor2");

        List<AuditDecision> history = auditService.getDecisionHistory("TXN001");
        assertEquals(2, history.size());
        assertEquals(d2.getId(), history.get(0).getId()); // newest first
        assertEquals(d1.getId(), history.get(1).getId());
    }

    @Test
    void testLatestDecisionReturnsTheNewestDecision() throws Exception {
        CreateAuditDecisionRequest req1 = new CreateAuditDecisionRequest(
                sampleAnalysisRunId,
                AuditDecisionType.REQUIRES_INVESTIGATION,
                "First",
                "Ignored"
        );
        auditService.recordDecision("TXN001", req1, "Auditor1");

        CreateAuditDecisionRequest req2 = new CreateAuditDecisionRequest(
                sampleAnalysisRunId,
                AuditDecisionType.FALSE_POSITIVE,
                "Second (Latest)",
                "Ignored"
        );
        AuditDecision d2 = auditService.recordDecision("TXN001", req2, "Auditor2");

        AuditDecision latest = auditService.getLatestDecision("TXN001");
        assertNotNull(latest);
        assertEquals(d2.getId(), latest.getId());
        assertEquals(AuditDecisionType.FALSE_POSITIVE, latest.getDecision());
        assertEquals("Second (Latest)", latest.getComment());
    }

    @Test
    void testAuditEventsAreCreatedWhenDecisionsAreCreated() throws Exception {
        CreateAuditDecisionRequest req = new CreateAuditDecisionRequest(
                sampleAnalysisRunId,
                AuditDecisionType.CONFIRMED_RISK,
                "Testing audit event creation",
                "Ignored"
        );
        AuditDecision decision = auditService.recordDecision("TXN001", req, "AuditorLead");

        List<AuditEvent> events = auditService.getAuditEventHistory("TXN001");
        assertEquals(1, events.size());
        AuditEvent event = events.get(0);
        assertEquals("TXN001", event.getTransactionId());
        assertEquals(decision.getId(), event.getDecisionId());
        assertEquals(AuditService.EVENT_TYPE_AUDIT_DECISION_CREATED, event.getEventType());
        assertTrue(event.getEventDetails().contains("CONFIRMED_RISK"));
        assertTrue(event.getEventDetails().contains(String.valueOf(sampleAnalysisRunId)));
        assertEquals("AuditorLead", event.getActor());
        assertNotNull(event.getCreatedAt());
    }

    @Test
    void testAuditEventHistoryIsNewestFirst() throws Exception {
        auditService.recordDecision("TXN001", new CreateAuditDecisionRequest(
                sampleAnalysisRunId, AuditDecisionType.REQUIRES_INVESTIGATION, "First event", "Ignored"), "Auditor1");

        auditService.recordDecision("TXN001", new CreateAuditDecisionRequest(
                sampleAnalysisRunId, AuditDecisionType.ESCALATED, "Second event", "Ignored"), "Auditor2");

        List<AuditEvent> events = auditService.getAuditEventHistory("TXN001");
        assertEquals(2, events.size());
        assertTrue(events.get(0).getEventDetails().contains("ESCALATED"));
        assertTrue(events.get(1).getEventDetails().contains("REQUIRES_INVESTIGATION"));
    }

    @Test
    void testTransactionRollbackWhenAuditEventFails() throws Exception {
        // Create an AuditService instance wrapped in Spring transaction proxy or test via MockBean/spy
        // In this test, we verify that when recordDecision fails during audit event creation, the transaction rolls back.
        // We can create a test transaction using TransactionTemplate or execute within transaction manager.
        org.springframework.transaction.PlatformTransactionManager txManager =
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(postgresDatabase());

        org.springframework.transaction.support.TransactionTemplate txTemplate =
                new org.springframework.transaction.support.TransactionTemplate(txManager);

        AuditEventRepository failingEventRepo = new AuditEventRepository(postgresDatabase()) {
            @Override
            public long save(String txnId, Long decId, String eventType, String eventDetails, String actor) {
                throw new RuntimeException("Simulated database failure during audit event save");
            }
        };

        AuditService serviceWithFault = new AuditService(
                auditServiceTransactionRepository(),
                riskAnalysisRunRepository,
                auditDecisionRepository,
                failingEventRepo
        );

        CreateAuditDecisionRequest req = new CreateAuditDecisionRequest(
                sampleAnalysisRunId,
                AuditDecisionType.CONFIRMED_RISK,
                "Should rollback",
                "Ignored"
        );

        assertThrows(RuntimeException.class, () ->
                txTemplate.execute(status -> {
                    try {
                        return serviceWithFault.recordDecision("TXN001", req, "AuditorRollback");
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
        );

        // Verify that audit_decisions contains NO record for this attempt
        List<AuditDecision> decisions = auditDecisionRepository.findByTransactionId("TXN001");
        assertTrue(decisions.isEmpty(), "Audit decision must be rolled back if audit event persistence fails");
    }

    @Test
    void testExistingRiskAnalysisScoreAndFindingsRemainUnchangedAfterAuditDecision() throws Exception {
        // Run risk analysis and persist
        RiskReport reportBefore = riskAnalysisService.analyzeAndPersistTransaction("TXN001");
        RiskAnalysisRun latestRun = riskAnalysisRunRepository.findLatestByTransactionId("TXN001");
        assertNotNull(latestRun);
        List<RiskFinding> findingsBefore = riskFindingRepository.findByAnalysisRunId(latestRun.getId());

        // Create an audit decision
        CreateAuditDecisionRequest req = new CreateAuditDecisionRequest(
                latestRun.getId(),
                AuditDecisionType.FALSE_POSITIVE,
                "Auditor marks false positive",
                "Ignored"
        );
        auditService.recordDecision("TXN001", req, "SeniorAuditor");

        // Verify risk analysis run remains identical
        RiskAnalysisRun runAfter = riskAnalysisRunRepository.findLatestByTransactionId("TXN001");
        assertNotNull(runAfter);
        assertEquals(latestRun.getId(), runAfter.getId());
        assertEquals(latestRun.getRiskScore(), runAfter.getRiskScore());
        assertEquals(latestRun.getRiskLevel(), runAfter.getRiskLevel());
        assertEquals(reportBefore.getRiskScore(), runAfter.getRiskScore());

        // Verify risk findings remain identical
        List<RiskFinding> findingsAfter = riskFindingRepository.findByAnalysisRunId(latestRun.getId());
        assertEquals(findingsBefore.size(), findingsAfter.size());
        for (int i = 0; i < findingsBefore.size(); i++) {
            assertEquals(findingsBefore.get(i).getType(), findingsAfter.get(i).getType());
            assertEquals(findingsBefore.get(i).getScore(), findingsAfter.get(i).getScore());
            assertEquals(findingsBefore.get(i).getSeverity(), findingsAfter.get(i).getSeverity());
            assertEquals(findingsBefore.get(i).getExplanation(), findingsAfter.get(i).getExplanation());
        }
    }

    private javax.sql.DataSource postgresDatabase() {
        return jdbcTemplate.getDataSource();
    }

    private repository.TransactionRepository auditServiceTransactionRepository() {
        return new repository.TransactionRepository(jdbcTemplate.getDataSource());
    }
}
