package com.vighnesh.auth;

import com.vighnesh.security.JwtService;
import com.vighnesh.user.User;
import com.vighnesh.user.UserDto;
import com.vighnesh.user.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank() ||
            request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadCredentialsException("Invalid username or password");
        }

        Optional<User> userOpt = userRepository.findByUsername(request.getUsername().trim());
        if (userOpt.isEmpty()) {
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = userOpt.get();
        if (!user.isEnabled()) {
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String token = jwtService.generateToken(user.getUsername(), user.getId(), user.getRole());
        long expiresIn = jwtService.getExpirationInSeconds();

        return new LoginResponse(token, "Bearer", expiresIn, UserDto.fromUser(user));
    }

    public UserDto getCurrentUser(String username) {
        if (username == null || username.isBlank()) {
            throw new BadCredentialsException("Unauthenticated request");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        return UserDto.fromUser(user);
    }
}
