package com.vighnesh.security;

import com.vighnesh.user.CreateUserRequest;
import com.vighnesh.user.UserRole;
import com.vighnesh.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class DevAdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevAdminBootstrap.class);

    private final UserService userService;
    private final Environment environment;

    public DevAdminBootstrap(UserService userService, Environment environment) {
        this.userService = userService;
        this.environment = environment;
    }

    @Override
    public void run(String... args) {
        boolean seedEnabled = Boolean.parseBoolean(
                environment.getProperty("aurex.security.seed-admin.enabled", "false"));

        if (!seedEnabled) {
            log.debug("Admin seeding is disabled.");
            return;
        }

        // Never automatically seed in production profile unless explicitly enabled by property
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            log.warn("Production profile active: Dev admin automatic bootstrap is disabled.");
            return;
        }

        // Read via Spring Environment only.
        // In production, StandardEnvironment includes SystemEnvironmentPropertySource so
        // AUREX_DEV_ADMIN_PASSWORD (OS env var) is already resolved into
        // aurex.security.dev-admin.password by application.properties.
        // In tests, MockEnvironment is fully controlled — System.getenv() would escape isolation.
        String adminPassword = environment.getProperty("aurex.security.dev-admin.password");

        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "Admin seeding is enabled (aurex.security.seed-admin.enabled=true), " +
                    "but no admin password was provided. You must set the AUREX_DEV_ADMIN_PASSWORD " +
                    "environment variable (or aurex.security.dev-admin.password property) to a secure password."
            );
        }

        if (userService.getUserCount() == 0) {
            try {
                CreateUserRequest request = new CreateUserRequest("admin", adminPassword, UserRole.ADMIN);
                userService.createUser(request);
                log.info("Development bootstrap: Successfully created initial local ADMIN user 'admin'.");
            } catch (Exception e) {
                log.warn("Development bootstrap notice: Initial admin creation skipped or failed: {}", e.getMessage());
            }
        }
    }
}
