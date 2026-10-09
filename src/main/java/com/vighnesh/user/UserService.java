package com.vighnesh.user;

import com.vighnesh.exception.UserConflictException;
import com.vighnesh.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long");
        }
        if (request.getRole() == null) {
            throw new IllegalArgumentException("Role is required");
        }

        String normalizedUsername = request.getUsername().trim();
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new UserConflictException("Username '" + normalizedUsername + "' already exists");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setPasswordHash(encodedPassword);
        user.setRole(request.getRole());
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.warn("Concurrent duplicate key conflict creating user with username='{}'", normalizedUsername);
            throw new UserConflictException("Username '" + normalizedUsername + "' already exists");
        }

        log.info("User created: username='{}', role={}", normalizedUsername, request.getRole());
        return UserDto.fromUser(savedUser);
    }

    @Transactional
    public UserDto updateUserStatus(Long id, boolean enabled, String currentAdminUsername) {
        if (id == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        // Self-management safety check
        if (currentAdminUsername != null) {
            String trimmedAdminUsername = currentAdminUsername.trim();
            boolean isSelf = targetUser.getUsername().equalsIgnoreCase(trimmedAdminUsername);
            if (!isSelf) {
                User currentAdmin = userRepository.findByUsername(trimmedAdminUsername).orElse(null);
                if (currentAdmin != null && currentAdmin.getId().equals(targetUser.getId())) {
                    isSelf = true;
                }
            }
            if (isSelf) {
                if (!enabled) {
                    log.warn("Rejected attempt to disable self: admin '{}' (ID: {}) tried to disable own account",
                            currentAdminUsername, targetUser.getId());
                    throw new UserConflictException("Cannot disable your own authenticated account");
                } else {
                    return UserDto.fromUser(targetUser);
                }
            }
        }

        // Last enabled ADMIN safety check: lock all ADMIN rows in deterministic ORDER BY id ASC to eliminate race conditions
        if (!enabled && targetUser.getRole() == UserRole.ADMIN) {
            List<User> adminRows = userRepository.findAdminsForUpdate();
            User currentTarget = adminRows.stream()
                    .filter(u -> u.getId().equals(id))
                    .findFirst()
                    .orElse(targetUser);

            if (!currentTarget.isEnabled()) {
                // Target is already disabled; idempotent no-op
                return UserDto.fromUser(currentTarget);
            }

            long enabledAdmins = adminRows.stream().filter(User::isEnabled).count();
            if (enabledAdmins <= 1) {
                log.warn("Rejected attempt to disable last enabled ADMIN: target user '{}' (ID: {}) by admin '{}'",
                        targetUser.getUsername(), targetUser.getId(), currentAdminUsername);
                throw new UserConflictException("Cannot disable the last enabled ADMIN account");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        userRepository.updateStatus(id, enabled, now);
        targetUser.setEnabled(enabled);
        targetUser.setUpdatedAt(now);

        if (enabled) {
            log.info("User enabled: username='{}', id={}, performed_by='{}'",
                    targetUser.getUsername(), targetUser.getId(), currentAdminUsername);
        } else {
            log.info("User disabled: username='{}', id={}, performed_by='{}'",
                    targetUser.getUsername(), targetUser.getId(), currentAdminUsername);
        }

        return UserDto.fromUser(targetUser);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserDto::fromUser)
                .collect(Collectors.toList());
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElse(null);
    }

    public UserDto getUserDtoByUsername(String username) {
        User user = findByUsername(username);
        return UserDto.fromUser(user);
    }

    public int getUserCount() {
        return userRepository.count();
    }
}
