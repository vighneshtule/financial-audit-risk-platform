package com.vighnesh.security;

import com.vighnesh.user.CreateUserRequest;
import com.vighnesh.user.UserRole;
import com.vighnesh.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * First-run administrator bootstrapper for production and staging environments.
 *
 * <p>Requirements:
 * <ul>
 *   <li>Runs only when {@code AUREX_PROD_INITIAL_ADMIN_PASSWORD} (or property
 *       {@code aurex.security.prod-admin.password}) is provided.</li>
 *   <li>Seeds an admin account only when {@link UserService#getUserCount()} is 0.</li>
 *   <li>Never overwrites, updates, or resets existing accounts.</li>
 *   <li>Throws an {@link IllegalStateException} if the bootstrap password is invalid
 *       (e.g. blank or shorter than 12 characters).</li>
 *   <li>Uses the application's BCrypt password encoder via {@link UserService#createUser}.</li>
 *   <li>Never logs the password, password hash, or secret tokens.</li>
 * </ul>
 */
@Component
@Order(10)
public class ProdAdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProdAdminBootstrap.class);

    public static final String PROD_ADMIN_PASSWORD_PROPERTY = "aurex.security.prod-admin.password";
    public static final String PROD_ADMIN_USERNAME_PROPERTY = "aurex.security.prod-admin.username";
    public static final String DEFAULT_PROD_ADMIN_USERNAME = "admin";
    public static final int MIN_PROD_PASSWORD_LENGTH = 12;

    private final UserService userService;
    private final Environment environment;

    public ProdAdminBootstrap(UserService userService, Environment environment) {
        this.userService = userService;
        this.environment = environment;
    }

    @Override
    public void run(String... args) {
        // Only execute in production profile to prevent running in dev/test even if env var is set
        if (!environment.matchesProfiles("prod")) {
            return;
        }

        String adminPassword = environment.getProperty(PROD_ADMIN_PASSWORD_PROPERTY);

        // If not supplied, production admin bootstrap is not requested.
        if (adminPassword == null || adminPassword.isEmpty()) {
            return;
        }

        // Fail-safe validation: password must be non-blank and meet minimum complexity/length
        if (adminPassword.isBlank() || adminPassword.length() < MIN_PROD_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "Production initial admin bootstrap failed: " +
                    PROD_ADMIN_PASSWORD_PROPERTY + " (or AUREX_PROD_INITIAL_ADMIN_PASSWORD) " +
                    "must be at least " + MIN_PROD_PASSWORD_LENGTH + " characters long."
            );
        }

        // Only seed if zero users exist in the database.
        int existingUserCount = userService.getUserCount();
        if (existingUserCount > 0) {
            log.info("Production initial admin bootstrap skipped: {} existing user(s) detected.", existingUserCount);
            return;
        }

        String username = environment.getProperty(PROD_ADMIN_USERNAME_PROPERTY, DEFAULT_PROD_ADMIN_USERNAME).trim();
        if (username.isBlank()) {
            username = DEFAULT_PROD_ADMIN_USERNAME;
        }

        try {
            CreateUserRequest request = new CreateUserRequest(username, adminPassword, UserRole.ADMIN);
            userService.createUser(request);
            log.info("Production bootstrap: Successfully created initial ADMIN user '{}'. " +
                    "Operator note: Please remove the bootstrap password environment variable from your deployment.",
                    username);
        } catch (com.vighnesh.exception.UserConflictException e) {
            // Concurrent startup safety: if another replica or process created the admin concurrently, log and continue.
            log.info("Production bootstrap: Initial admin '{}' was created concurrently by another process.", username);
        } catch (Exception e) {
            log.error("Production bootstrap: Failed to create initial admin user '{}': {}", username, e.getMessage());
            throw new IllegalStateException("Production initial admin bootstrap failed to persist user", e);
        }
    }
}
