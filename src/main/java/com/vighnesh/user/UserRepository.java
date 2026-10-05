package com.vighnesh.user;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(UserRole.valueOf(rs.getString("role")));
        user.setEnabled(rs.getBoolean("enabled"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return user;
    };

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, role, enabled, created_at, updated_at FROM users WHERE username = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, username);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findById(Long id) {
        String sql = "SELECT id, username, password_hash, role, enabled, created_at, updated_at FROM users WHERE id = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, id);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username);
        return count != null && count > 0;
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM users";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    public User save(User user) {
        if (user.getId() == null) {
            String sql = "INSERT INTO users (username, password_hash, role, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();

            LocalDateTime now = LocalDateTime.now();
            if (user.getCreatedAt() == null) {
                user.setCreatedAt(now);
            }
            if (user.getUpdatedAt() == null) {
                user.setUpdatedAt(now);
            }

            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getPasswordHash());
                ps.setString(3, user.getRole().name());
                ps.setBoolean(4, user.isEnabled());
                ps.setTimestamp(5, Timestamp.valueOf(user.getCreatedAt()));
                ps.setTimestamp(6, Timestamp.valueOf(user.getUpdatedAt()));
                return ps;
            }, keyHolder);

            Number key = null;
            if (keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id")) {
                key = (Number) keyHolder.getKeys().get("id");
            } else if (keyHolder.getKeyList() != null && !keyHolder.getKeyList().isEmpty()) {
                key = (Number) keyHolder.getKeyList().get(0).get("id");
            } else {
                try {
                    key = keyHolder.getKey();
                } catch (Exception ignored) {
                }
            }
            if (key != null) {
                user.setId(key.longValue());
            }
            return user;
        } else {
            String sql = "UPDATE users SET username = ?, password_hash = ?, role = ?, enabled = ?, updated_at = ? WHERE id = ?";
            user.setUpdatedAt(LocalDateTime.now());
            jdbcTemplate.update(sql,
                    user.getUsername(),
                    user.getPasswordHash(),
                    user.getRole().name(),
                    user.isEnabled(),
                    Timestamp.valueOf(user.getUpdatedAt()),
                    user.getId()
            );
            return user;
        }
    }

    public List<User> findAll() {
        String sql = "SELECT id, username, password_hash, role, enabled, created_at, updated_at FROM users ORDER BY id ASC";
        return jdbcTemplate.query(sql, userRowMapper);
    }
}
