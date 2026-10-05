package com.vighnesh.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vighnesh.FinancialAuditRiskApplication;
import com.vighnesh.auth.LoginRequest;
import com.vighnesh.user.CreateUserRequest;
import com.vighnesh.user.User;
import com.vighnesh.user.UserRepository;
import com.vighnesh.user.UserRole;
import com.vighnesh.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = FinancialAuditRiskApplication.class)
@AutoConfigureMockMvc
public class SecurityTests {

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
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:securitytestdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;
    private String auditorToken;
    private String viewerToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM users");

        // Seed users
        userService.createUser(new CreateUserRequest("admin_user", "AdminPass123", UserRole.ADMIN));
        userService.createUser(new CreateUserRequest("auditor_user", "AuditorPass123", UserRole.AUDITOR));
        userService.createUser(new CreateUserRequest("viewer_user", "ViewerPass123", UserRole.VIEWER));

        User admin = userService.findByUsername("admin_user");
        User auditor = userService.findByUsername("auditor_user");
        User viewer = userService.findByUsername("viewer_user");

        adminToken = jwtService.generateToken(admin.getUsername(), admin.getId(), admin.getRole());
        auditorToken = jwtService.generateToken(auditor.getUsername(), auditor.getId(), auditor.getRole());
        viewerToken = jwtService.generateToken(viewer.getUsername(), viewer.getId(), viewer.getRole());
    }

    @Nested
    @DisplayName("A. Password Hashing Tests")
    class PasswordHashingTests {

        @Test
        void passwordIsNotStoredPlaintext() {
            Optional<User> userOpt = userRepository.findByUsername("admin_user");
            assertTrue(userOpt.isPresent());
            User user = userOpt.get();

            assertNotEquals("AdminPass123", user.getPasswordHash());
            assertTrue(user.getPasswordHash().startsWith("$2a$") || user.getPasswordHash().startsWith("$2b$"));
            assertTrue(passwordEncoder.matches("AdminPass123", user.getPasswordHash()));
        }
    }

    @Nested
    @DisplayName("B. Login Endpoint Tests")
    class LoginTests {

        @Test
        void validCredentialsReturns200AndTokenAndUserDto() throws Exception {
            LoginRequest request = new LoginRequest("admin_user", "AdminPass123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token", notNullValue()))
                    .andExpect(jsonPath("$.tokenType", is("Bearer")))
                    .andExpect(jsonPath("$.expiresIn", greaterThan(0)))
                    .andExpect(jsonPath("$.user.username", is("admin_user")))
                    .andExpect(jsonPath("$.user.role", is("ADMIN")))
                    .andExpect(jsonPath("$.user.password").doesNotExist())
                    .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
        }

        @Test
        void invalidPasswordReturns401WithGenericMessage() throws Exception {
            LoginRequest request = new LoginRequest("admin_user", "WrongPassword");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status", is(401)))
                    .andExpect(jsonPath("$.message", is("Invalid username or password")));
        }

        @Test
        void unknownUsernameReturns401WithGenericMessage() throws Exception {
            LoginRequest request = new LoginRequest("non_existent_user", "AnyPassword");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status", is(401)))
                    .andExpect(jsonPath("$.message", is("Invalid username or password")));
        }
    }

    @Nested
    @DisplayName("C. JWT Token Validation Tests")
    class JwtValidationTests {

        @Test
        void validTokenAuthenticatesRequest() throws Exception {
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username", is("admin_user")))
                    .andExpect(jsonPath("$.role", is("ADMIN")));
        }

        @Test
        void invalidTokenIsRejected() throws Exception {
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer invalid_jwt_token_format"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status", is(401)));
        }

        @Test
        void missingTokenIsRejectedOnProtectedEndpoint() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status", is(401)));
        }
    }

    @Nested
    @DisplayName("D. Current User Endpoint (/api/auth/me) Tests")
    class CurrentUserEndpointTests {

        @Test
        void authenticatedUserCanAccessMe() throws Exception {
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer " + auditorToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username", is("auditor_user")))
                    .andExpect(jsonPath("$.role", is("AUDITOR")));
        }

        @Test
        void unauthenticatedUserGets401OnMe() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("E. Role-Based Authorization Tests")
    class AuthorizationMatrixTests {

        @Test
        void adminCanAccessUserManagement() throws Exception {
            mockMvc.perform(get("/api/users")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        void adminCanCreateUser() throws Exception {
            CreateUserRequest req = new CreateUserRequest("new_auditor", "Pass123!", UserRole.AUDITOR);

            mockMvc.perform(post("/api/users")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username", is("new_auditor")))
                    .andExpect(jsonPath("$.role", is("AUDITOR")))
                    .andExpect(jsonPath("$.password").doesNotExist())
                    .andExpect(jsonPath("$.passwordHash").doesNotExist());
        }

        @Test
        void auditorCannotAccessUserManagement() throws Exception {
            mockMvc.perform(get("/api/users")
                            .header("Authorization", "Bearer " + auditorToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)));
        }

        @Test
        void auditorCanAccessImportAndAnalyze() throws Exception {
            mockMvc.perform(get("/api/transactions")
                            .header("Authorization", "Bearer " + auditorToken))
                    .andExpect(status().isOk());
        }

        @Test
        void viewerCanReadTransactionsAndRisk() throws Exception {
            mockMvc.perform(get("/api/transactions")
                            .header("Authorization", "Bearer " + viewerToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/risk/summary")
                            .header("Authorization", "Bearer " + viewerToken))
                    .andExpect(status().isOk());
        }

        @Test
        void viewerCannotImportTransactions() throws Exception {
            mockMvc.perform(post("/api/transactions/import")
                            .header("Authorization", "Bearer " + viewerToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)));
        }

        @Test
        void viewerCannotRunRiskAnalysis() throws Exception {
            mockMvc.perform(post("/api/risk/analyze/TXN001")
                            .header("Authorization", "Bearer " + viewerToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)));
        }

        @Test
        void viewerCannotCreateAuditDecision() throws Exception {
            String decisionJson = """
                    {
                        "analysisRunId": 1,
                        "decision": "APPROVED",
                        "comment": "Test decision",
                        "decidedBy": "viewer"
                    }
                    """;

            mockMvc.perform(post("/api/risk/transactions/TXN001/audit/decisions")
                            .header("Authorization", "Bearer " + viewerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(decisionJson))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)));
        }

        @Test
        void viewerCannotManageUsers() throws Exception {
            mockMvc.perform(get("/api/users")
                            .header("Authorization", "Bearer " + viewerToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("F. Public Endpoint Tests")
    class PublicEndpointTests {

        @Test
        void healthEndpointIsPublic() throws Exception {
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("UP")));
        }

        @Test
        void loginEndpointIsPublic() throws Exception {
            LoginRequest req = new LoginRequest("admin_user", "AdminPass123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("G. Dev Admin Bootstrap Hardening Tests")
    class DevAdminBootstrapTests {

        @Test
        void bootstrapFailsWhenSeedingEnabledWithoutPassword() {
            org.springframework.mock.env.MockEnvironment env = new org.springframework.mock.env.MockEnvironment();
            env.setProperty("aurex.security.seed-admin.enabled", "true");

            DevAdminBootstrap bootstrap = new DevAdminBootstrap(userService, env);

            IllegalStateException ex = assertThrows(IllegalStateException.class, bootstrap::run);
            assertTrue(ex.getMessage().contains("Admin seeding is enabled"));
            assertTrue(ex.getMessage().contains("AUREX_DEV_ADMIN_PASSWORD"));
        }

        @Test
        void bootstrapDoesNothingWhenSeedingDisabled() throws Exception {
            org.springframework.mock.env.MockEnvironment env = new org.springframework.mock.env.MockEnvironment();
            env.setProperty("aurex.security.seed-admin.enabled", "false");

            DevAdminBootstrap bootstrap = new DevAdminBootstrap(userService, env);
            assertDoesNotThrow((org.junit.jupiter.api.function.Executable) () -> bootstrap.run());
        }

        @Test
        void bootstrapDoesNothingInProductionProfile() throws Exception {
            org.springframework.mock.env.MockEnvironment env = new org.springframework.mock.env.MockEnvironment();
            env.setActiveProfiles("prod");
            env.setProperty("aurex.security.seed-admin.enabled", "true");
            env.setProperty("aurex.security.dev-admin.password", "ValidSecurePass123!");

            DevAdminBootstrap bootstrap = new DevAdminBootstrap(userService, env);
            assertDoesNotThrow((org.junit.jupiter.api.function.Executable) () -> bootstrap.run());
        }
    }
}
