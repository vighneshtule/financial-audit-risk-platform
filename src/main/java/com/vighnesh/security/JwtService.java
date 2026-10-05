package com.vighnesh.security;

import com.vighnesh.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String DEFAULT_DEV_SECRET = "dev_aurex_super_secret_key_minimum_32_bytes_long_for_hmac_sha256";

    @Value("${jwt.secret:dev_aurex_super_secret_key_minimum_32_bytes_long_for_hmac_sha256}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400}")
    private long jwtExpirationInSeconds;

    private final Environment environment;
    private SecretKey key;

    public JwtService(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            if (jwtSecret == null || jwtSecret.isBlank() || DEFAULT_DEV_SECRET.equals(jwtSecret) || jwtSecret.length() < 32) {
                throw new IllegalStateException("Production environment error: JWT_SECRET environment variable must be set to a secure key of at least 32 characters.");
            }
        }

        if (jwtSecret == null || jwtSecret.length() < 32) {
            log.warn("JWT secret is shorter than 32 bytes; using padded key for development.");
            String padded = (jwtSecret + DEFAULT_DEV_SECRET).substring(0, 32);
            this.key = Keys.hmacShaKeyFor(padded.getBytes(StandardCharsets.UTF_8));
        } else {
            this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        }
    }

    public String generateToken(String username, Long userId, UserRole role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (jwtExpirationInSeconds * 1000));

        return Jwts.builder()
                .subject(username)
                .claim("id", userId)
                .claim("role", role != null ? role.name() : null)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    public UserRole getRoleFromToken(String token) {
        Claims claims = getClaims(token);
        if (claims != null && claims.get("role") != null) {
            try {
                return UserRole.valueOf(claims.get("role", String.class));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        if (claims != null && claims.get("id") != null) {
            Number id = claims.get("id", Number.class);
            return id != null ? id.longValue() : null;
        }
        return null;
    }

    public long getExpirationInSeconds() {
        return jwtExpirationInSeconds;
    }

    private Claims getClaims(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return jws.getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
