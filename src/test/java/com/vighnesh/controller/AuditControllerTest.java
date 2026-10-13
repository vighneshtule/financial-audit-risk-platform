package com.vighnesh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vighnesh.FinancialAuditRiskApplication;
import model.AuditDecisionType;
import model.CreateAuditDecisionRequest;
import model.RiskSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.springframework.security.test.context.support.WithMockUser;
import repository.RiskAnalysisRunRepository;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = FinancialAuditRiskApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = {"ADMIN"})
class AuditControllerTest {

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
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:auditdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RiskAnalysisRunRepository riskAnalysisRunRepository;

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
    void testCreateDecisionReturns201Created() throws Exception {
        CreateAuditDecisionRequest req = new CreateAuditDecisionRequest(
                sampleAnalysisRunId,
                AuditDecisionType.FALSE_POSITIVE,
                "Known scheduled reconciliation",
                "Auditor"
        );

        mockMvc.perform(post("/api/risk/transactions/TXN001/audit/decisions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.transactionId", is("TXN001")))
                .andExpect(jsonPath("$.decision", is("FALSE_POSITIVE")))
                .andExpect(jsonPath("$.comment", is("Known scheduled reconciliation")))
                .andExpect(jsonPath("$.decidedBy", is("admin")))
                .andExpect(jsonPath("$.decidedAt", notNullValue()));
    }

    @Test
    void testCreateDecisionIgnoresClientSuppliedDecidedBy() throws Exception {
        // Even if client sends "CFO_Impersonator" or omits decidedBy entirely,
        // the server records the authenticated principal ("admin")
        String body = """
                {
                    "analysisRunId": %d,
                    "decision": "CONFIRMED_RISK",
                    "comment": "Spoofed decidedBy",
                    "decidedBy": "CFO_Impersonator"
                }
                """.formatted(sampleAnalysisRunId);

        mockMvc.perform(post("/api/risk/transactions/TXN001/audit/decisions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.decidedBy", is("admin")));
    }

    @Test
    void testCreateDecisionFailsClosedWhenPrincipalAbsent() throws Exception {
        CreateAuditDecisionRequest req = new CreateAuditDecisionRequest(
                sampleAnalysisRunId,
                AuditDecisionType.CONFIRMED_RISK,
                "Attempt with unauthenticated context",
                "Auditor"
        );

        // Perform request explicitly without mock user/principal authentication
        mockMvc.perform(post("/api/risk/transactions/TXN001/audit/decisions")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateDecisionRecordsAuthenticatedUsernameInDecisionAndEvent() throws Exception {
        CreateAuditDecisionRequest req = new CreateAuditDecisionRequest(
                sampleAnalysisRunId,
                AuditDecisionType.CONFIRMED_RISK,
                "Integrity test for actor persistence",
                "AttackerSuppliedName"
        );

        // Authenticated user is "admin"
        mockMvc.perform(post("/api/risk/transactions/TXN001/audit/decisions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.decidedBy", is("admin")));

        // Check latest decision
        mockMvc.perform(get("/api/risk/transactions/TXN001/audit/decision"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decidedBy", is("admin")));

        // Check audit event
        mockMvc.perform(get("/api/risk/transactions/TXN001/audit/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actor", is("admin")));
    }

    @Test
    void testCreateDecisionReturns404WhenTransactionNotFound() throws Exception {
        CreateAuditDecisionRequest req = new CreateAuditDecisionRequest(
                sampleAnalysisRunId,
                AuditDecisionType.CONFIRMED_RISK,
                "Testing missing transaction",
                "Auditor"
        );

        mockMvc.perform(post("/api/risk/transactions/UNKNOWN_TXN/audit/decisions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void testGetDecisionsAndLatestDecisionAndEvents() throws Exception {
        // 1. Post two decisions
        CreateAuditDecisionRequest req1 = new CreateAuditDecisionRequest(
                sampleAnalysisRunId,
                AuditDecisionType.REQUIRES_INVESTIGATION,
                "Initial review",
                "JuniorAuditor"
        );
        mockMvc.perform(post("/api/risk/transactions/TXN001/audit/decisions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        CreateAuditDecisionRequest req2 = new CreateAuditDecisionRequest(
                sampleAnalysisRunId,
                AuditDecisionType.CONFIRMED_RISK,
                "Final confirmed risk",
                "LeadAuditor"
        );
        mockMvc.perform(post("/api/risk/transactions/TXN001/audit/decisions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated());

        // 2. GET /decisions -> returns 2 decisions, newest first
        mockMvc.perform(get("/api/risk/transactions/TXN001/audit/decisions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].decision", is("CONFIRMED_RISK")))
                .andExpect(jsonPath("$[0].decidedBy", is("admin")))
                .andExpect(jsonPath("$[1].decision", is("REQUIRES_INVESTIGATION")))
                .andExpect(jsonPath("$[1].decidedBy", is("admin")));

        // 3. GET /decision -> returns latest decision
        mockMvc.perform(get("/api/risk/transactions/TXN001/audit/decision"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision", is("CONFIRMED_RISK")))
                .andExpect(jsonPath("$.decidedBy", is("admin")))
                .andExpect(jsonPath("$.comment", is("Final confirmed risk")));

        // 4. GET /events -> returns 2 audit events
        mockMvc.perform(get("/api/risk/transactions/TXN001/audit/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].eventType", is("AUDIT_DECISION_CREATED")))
                .andExpect(jsonPath("$[0].eventDetails", containsString("CONFIRMED_RISK")))
                .andExpect(jsonPath("$[1].eventType", is("AUDIT_DECISION_CREATED")))
                .andExpect(jsonPath("$[1].eventDetails", containsString("REQUIRES_INVESTIGATION")));
    }
}
