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

import com.vighnesh.user.UpdateUserStatusRequest;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @Nested
    @DisplayName("H. Sprint 2B.6 Admin User Management Tests")
    class AdminUserManagementTests {

        @Test
        @DisplayName("A. ADMIN can list users")
        void adminCanListUsers() throws Exception {
            mockMvc.perform(get("/api/users")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                    .andExpect(jsonPath("$[0].username", notNullValue()))
                    .andExpect(jsonPath("$[0].role", notNullValue()))
                    .andExpect(jsonPath("$[0].enabled", notNullValue()));
        }

        @Test
        @DisplayName("B. AUDITOR cannot list users")
        void auditorCannotListUsers() throws Exception {
            mockMvc.perform(get("/api/users")
                            .header("Authorization", "Bearer " + auditorToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)));
        }

        @Test
        @DisplayName("C. VIEWER cannot list users")
        void viewerCannotListUsers() throws Exception {
            mockMvc.perform(get("/api/users")
                            .header("Authorization", "Bearer " + viewerToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)));
        }

        @Test
        @DisplayName("D. ADMIN can create AUDITOR")
        void adminCanCreateAuditor() throws Exception {
            CreateUserRequest req = new CreateUserRequest("auditor_new_user", "SecurePass123!", UserRole.AUDITOR);

            mockMvc.perform(post("/api/users")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username", is("auditor_new_user")))
                    .andExpect(jsonPath("$.role", is("AUDITOR")))
                    .andExpect(jsonPath("$.enabled", is(true)));
        }

        @Test
        @DisplayName("E. ADMIN can create VIEWER")
        void adminCanCreateViewer() throws Exception {
            CreateUserRequest req = new CreateUserRequest("viewer_new_user", "SecurePass123!", UserRole.VIEWER);

            mockMvc.perform(post("/api/users")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username", is("viewer_new_user")))
                    .andExpect(jsonPath("$.role", is("VIEWER")))
                    .andExpect(jsonPath("$.enabled", is(true)));
        }

        @Test
        @DisplayName("F. Duplicate username returns 409")
        void duplicateUsernameReturns409() throws Exception {
            CreateUserRequest req = new CreateUserRequest("admin_user", "AnotherPass123!", UserRole.AUDITOR);

            mockMvc.perform(post("/api/users")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.message", containsString("already exists")));
        }

        @Test
        @DisplayName("G. Password is never returned in API responses")
        void passwordIsNeverReturned() throws Exception {
            CreateUserRequest req = new CreateUserRequest("safe_auditor", "SecretPass123!", UserRole.AUDITOR);

            mockMvc.perform(post("/api/users")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.password").doesNotExist())
                    .andExpect(jsonPath("$.passwordHash").doesNotExist())
                    .andExpect(jsonPath("$.password_hash").doesNotExist());

            mockMvc.perform(get("/api/users")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].password").doesNotExist())
                    .andExpect(jsonPath("$[*].passwordHash").doesNotExist())
                    .andExpect(jsonPath("$[*].password_hash").doesNotExist());
        }

        @Test
        @DisplayName("H. ADMIN can disable another user")
        void adminCanDisableAnotherUser() throws Exception {
            User auditor = userService.findByUsername("auditor_user");
            UpdateUserStatusRequest req = new UpdateUserStatusRequest(false);

            mockMvc.perform(patch("/api/users/" + auditor.getId() + "/status")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(auditor.getId().intValue())))
                    .andExpect(jsonPath("$.enabled", is(false)));

            User updated = userService.findByUsername("auditor_user");
            assertFalse(updated.isEnabled());
        }

        @Test
        @DisplayName("H2. Disabled user cannot authenticate via login")
        void disabledUserCannotAuthenticate() throws Exception {
            User auditor = userService.findByUsername("auditor_user");
            userService.updateUserStatus(auditor.getId(), false, "admin_user");

            LoginRequest loginReq = new LoginRequest("auditor_user", "AuditorPass123");
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginReq)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status", is(401)))
                    .andExpect(jsonPath("$.message", is("Invalid username or password")));
        }

        @Test
        @DisplayName("Sprint 2B.6 13-step End-to-End User Management Workflow")
        void fullUserManagementWorkflow() throws Exception {
            // 1. login as ADMIN
            LoginRequest adminLogin = new LoginRequest("admin_user", "AdminPass123");
            String adminLoginRes = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(adminLogin)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String currentAdminToken = objectMapper.readTree(adminLoginRes).get("token").asText();

            // 2. GET /api/users → 200
            mockMvc.perform(get("/api/users")
                            .header("Authorization", "Bearer " + currentAdminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))));

            // 3. create AUDITOR
            CreateUserRequest createAuditor = new CreateUserRequest("e2e_auditor", "AuditorPass123!", UserRole.AUDITOR);
            String auditorRes = mockMvc.perform(post("/api/users")
                            .header("Authorization", "Bearer " + currentAdminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createAuditor)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username", is("e2e_auditor")))
                    .andExpect(jsonPath("$.role", is("AUDITOR")))
                    .andExpect(jsonPath("$.enabled", is(true)))
                    .andReturn().getResponse().getContentAsString();
            long newAuditorId = objectMapper.readTree(auditorRes).get("id").asLong();

            // 4. create VIEWER
            CreateUserRequest createViewer = new CreateUserRequest("e2e_viewer", "ViewerPass123!", UserRole.VIEWER);
            mockMvc.perform(post("/api/users")
                            .header("Authorization", "Bearer " + currentAdminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createViewer)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username", is("e2e_viewer")))
                    .andExpect(jsonPath("$.role", is("VIEWER")))
                    .andExpect(jsonPath("$.enabled", is(true)));

            // 5. login as AUDITOR
            LoginRequest auditorLogin = new LoginRequest("e2e_auditor", "AuditorPass123!");
            String auditorLoginRes = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(auditorLogin)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String e2eAuditorToken = objectMapper.readTree(auditorLoginRes).get("token").asText();

            // 6. GET /api/users → 403
            mockMvc.perform(get("/api/users")
                            .header("Authorization", "Bearer " + e2eAuditorToken))
                    .andExpect(status().isForbidden());

            // 7. login as VIEWER
            LoginRequest viewerLogin = new LoginRequest("e2e_viewer", "ViewerPass123!");
            String viewerLoginRes = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(viewerLogin)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String e2eViewerToken = objectMapper.readTree(viewerLoginRes).get("token").asText();

            // 8. GET /api/users → 403
            mockMvc.perform(get("/api/users")
                            .header("Authorization", "Bearer " + e2eViewerToken))
                    .andExpect(status().isForbidden());

            // 9. login as ADMIN
            // currentAdminToken verified

            // 10. disable AUDITOR
            UpdateUserStatusRequest disableReq = new UpdateUserStatusRequest(false);
            mockMvc.perform(patch("/api/users/" + newAuditorId + "/status")
                            .header("Authorization", "Bearer " + currentAdminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(disableReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled", is(false)));

            // 11. verify disabled user cannot authenticate
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(auditorLogin)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message", is("Invalid username or password")));

            // 12. verify ADMIN cannot disable self
            User admin = userService.findByUsername("admin_user");
            mockMvc.perform(patch("/api/users/" + admin.getId() + "/status")
                            .header("Authorization", "Bearer " + currentAdminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(disableReq)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)));

            // 13. verify last enabled ADMIN cannot be disabled
            assertThrows(com.vighnesh.exception.UserConflictException.class, () ->
                    userService.updateUserStatus(admin.getId(), false, "other_admin")
            );
        }

        @Test
        @DisplayName("I. ADMIN can enable another user")
        void adminCanEnableAnotherUser() throws Exception {
            User auditor = userService.findByUsername("auditor_user");
            userService.updateUserStatus(auditor.getId(), false, "admin_user");

            UpdateUserStatusRequest req = new UpdateUserStatusRequest(true);

            mockMvc.perform(patch("/api/users/" + auditor.getId() + "/status")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(auditor.getId().intValue())))
                    .andExpect(jsonPath("$.enabled", is(true)));

            User updated = userService.findByUsername("auditor_user");
            assertTrue(updated.isEnabled());
        }

        @Test
        @DisplayName("J. ADMIN cannot disable self")
        void adminCannotDisableSelf() throws Exception {
            User admin = userService.findByUsername("admin_user");
            UpdateUserStatusRequest req = new UpdateUserStatusRequest(false);

            mockMvc.perform(patch("/api/users/" + admin.getId() + "/status")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.message", containsString("Cannot disable your own")));

            User stillAdmin = userService.findByUsername("admin_user");
            assertTrue(stillAdmin.isEnabled());
        }

        @Test
        @DisplayName("K. Cannot disable last enabled ADMIN")
        void cannotDisableLastEnabledAdmin() throws Exception {
            User admin = userService.findByUsername("admin_user");
            UpdateUserStatusRequest req = new UpdateUserStatusRequest(false);

            userService.createUser(new CreateUserRequest("admin_two", "AdminTwoPass123!", UserRole.ADMIN));
            User adminTwo = userService.findByUsername("admin_two");
            String adminTwoToken = jwtService.generateToken(adminTwo.getUsername(), adminTwo.getId(), adminTwo.getRole());

            mockMvc.perform(patch("/api/users/" + admin.getId() + "/status")
                            .header("Authorization", "Bearer " + adminTwoToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled", is(false)));

            mockMvc.perform(patch("/api/users/" + adminTwo.getId() + "/status")
                            .header("Authorization", "Bearer " + adminTwoToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict());

            assertThrows(com.vighnesh.exception.UserConflictException.class, () ->
                    userService.updateUserStatus(adminTwo.getId(), false, "other_caller")
            );
        }

        @Test
        @DisplayName("L. AUDITOR cannot create user")
        void auditorCannotCreateUser() throws Exception {
            CreateUserRequest req = new CreateUserRequest("some_user", "SomePass123!", UserRole.VIEWER);

            mockMvc.perform(post("/api/users")
                            .header("Authorization", "Bearer " + auditorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)));
        }

        @Test
        @DisplayName("M. VIEWER cannot create user")
        void viewerCannotCreateUser() throws Exception {
            CreateUserRequest req = new CreateUserRequest("some_user", "SomePass123!", UserRole.VIEWER);

            mockMvc.perform(post("/api/users")
                            .header("Authorization", "Bearer " + viewerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)));
        }

        @Test
        @DisplayName("N. Nonexistent user status update returns 404")
        void nonexistentUserStatusUpdateReturns404() throws Exception {
            UpdateUserStatusRequest req = new UpdateUserStatusRequest(false);

            mockMvc.perform(patch("/api/users/999999/status")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", containsString("User not found")));
        }

        @Test
        @DisplayName("O. Concurrent requests to disable last two enabled ADMINs are serialized and last ADMIN is preserved")
        void concurrentLastAdminDisablePreservesAtLeastOneAdmin() throws Exception {
            // Ensure we have exactly 2 enabled admins: admin_user and admin_concurrent_two
            User adminOne = userService.findByUsername("admin_user");
            if (!adminOne.isEnabled()) {
                userService.updateUserStatus(adminOne.getId(), true, "system");
            }

            User adminTwo = userService.findByUsername("admin_concurrent_two");
            if (adminTwo == null) {
                userService.createUser(new CreateUserRequest("admin_concurrent_two", "AdminTwoPass123!", UserRole.ADMIN));
                adminTwo = userService.findByUsername("admin_concurrent_two");
            } else if (!adminTwo.isEnabled()) {
                userService.updateUserStatus(adminTwo.getId(), true, "system");
            }

            final Long adminOneId = adminOne.getId();
            final Long adminTwoId = adminTwo.getId();

            java.util.concurrent.CountDownLatch readyLatch = new java.util.concurrent.CountDownLatch(2);
            java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger conflictCount = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger otherErrorCount = new java.util.concurrent.atomic.AtomicInteger(0);

            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);

            // Thread 1: admin_user tries to disable admin_concurrent_two
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    userService.updateUserStatus(adminTwoId, false, "admin_user");
                    successCount.incrementAndGet();
                } catch (com.vighnesh.exception.UserConflictException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    otherErrorCount.incrementAndGet();
                }
            });

            // Thread 2: admin_concurrent_two tries to disable admin_user
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    userService.updateUserStatus(adminOneId, false, "admin_concurrent_two");
                    successCount.incrementAndGet();
                } catch (com.vighnesh.exception.UserConflictException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    otherErrorCount.incrementAndGet();
                }
            });

            readyLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);
            startLatch.countDown();
            executor.shutdown();
            executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

            assertEquals(0, otherErrorCount.get(), "Unexpected errors during concurrent status updates");
            assertEquals(1, successCount.get(), "Exactly one disable operation must succeed");
            assertEquals(1, conflictCount.get(), "Exactly one disable operation must be rejected with 409 conflict");

            // Verify database state: at least 1 enabled admin remains
            int remainingEnabledAdmins = userRepository.countEnabledAdmins();
            assertTrue(remainingEnabledAdmins >= 1, "At least one enabled admin must remain in the database");

            // Clean up: re-enable so other tests aren't affected
            adminOne = userService.findByUsername("admin_user");
            if (!adminOne.isEnabled()) {
                userService.updateUserStatus(adminOneId, true, "system");
            }
            adminTwo = userService.findByUsername("admin_concurrent_two");
            if (!adminTwo.isEnabled()) {
                userService.updateUserStatus(adminTwoId, true, "system");
            }
        }
    }
}
