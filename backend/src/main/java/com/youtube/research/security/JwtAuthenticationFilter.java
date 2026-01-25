package com.youtube.research.security;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private static final String BEARER_PREFIX = "Bearer ";

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        // No authorization header - continue without authentication
        if (authHeader == null) {
            log.debug("No Authorization header found");
            return chain.filter(exchange);
        }

        // Check Bearer prefix
        if (!authHeader.startsWith(BEARER_PREFIX)) {
            log.debug("Authorization header does not start with 'Bearer '");
            return chain.filter(exchange);
        }

        try {
            // Extract token
            String token = authHeader.substring(BEARER_PREFIX.length());

            if (token.isEmpty()) {
                log.debug("Token is empty after Bearer prefix");
                return chain.filter(exchange);
            }

            // Validate token format (must be JWT with 3 parts: header.payload.signature)
            if (!isValidJwtFormat(token)) {
                log.warn("Invalid JWT format");
                return chain.filter(exchange);
            }

            // Validate token signature, expiration, issuer, audience, etc.
            if (!jwtTokenProvider.validateToken(token)) {
                log.debug("Token validation failed");
                return chain.filter(exchange);
            }

            // Extract username from token
            String username = jwtTokenProvider.extractUsername(token);

            if (username == null || username.isEmpty()) {
                log.warn("Token does not contain username claim");
                return chain.filter(exchange);
            }

            // Verify token type is "access" (not refresh or other type)
            if (!jwtTokenProvider.isTokenType(token, "access")) {
                log.warn("Token type is not 'access'");
                return chain.filter(exchange);
            }

            // Create authentication object with ROLE_USER authority
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, token, authorities);

            log.debug("Successfully authenticated user: {}", username);

            // Set authentication in reactive context and continue filter chain
            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));

        } catch (JwtException e) {
            log.warn("JWT processing error: {}", e.getMessage());
            return chain.filter(exchange);
        } catch (Exception e) {
            log.error("Unexpected error during JWT processing: {}", e.getMessage(), e);
            return chain.filter(exchange);
        }
    }

    /**
     * Validate JWT format (must have 3 parts separated by dots)
     *
     * @param token JWT token
     * @return true if format is valid
     */
    private boolean isValidJwtFormat(String token) {
        return token.matches("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$");
    }
}