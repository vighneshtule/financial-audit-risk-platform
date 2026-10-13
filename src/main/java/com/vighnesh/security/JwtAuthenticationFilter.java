package com.vighnesh.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService customUserDetailsService) {
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty() || !jwtService.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = jwtService.getUsernameFromToken(token);
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Always load UserDetails from the database so that the live `enabled`
            // flag is checked on every request. This ensures that an account
            // disabled after token issuance is refused immediately (HTTP 401)
            // rather than remaining active for the full token lifetime.
            //
            // UsernameNotFoundException or DisabledException must NOT propagate as
            // HTTP 500. We treat them as authentication failures: leave the
            // SecurityContext empty so the AuthenticationEntryPoint returns 401.
            UserDetails userDetails;
            try {
                userDetails = customUserDetailsService.loadUserByUsername(username);
            } catch (UsernameNotFoundException | DisabledException e) {
                // Account deleted or disabled since the token was issued.
                // Leave SecurityContext unauthenticated -> AuthenticationEntryPoint -> 401.
                filterChain.doFilter(request, response);
                return;
            }

            // Explicitly enforce the enabled flag. Spring's DaoAuthenticationProvider
            // does this during form login but not during manual filter-level
            // authentication token construction.
            if (!userDetails.isEnabled()) {
                filterChain.doFilter(request, response);
                return;
            }

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}
