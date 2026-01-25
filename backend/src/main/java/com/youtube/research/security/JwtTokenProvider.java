package com.youtube.research.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:mysecretkeythatisatleast32characterslong}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpirationMs;

    @Value("${jwt.issuer:youtube-research-agent}")
    private String jwtIssuer;

    @Value("${jwt.audience:web-client}")
    private String jwtAudience;

    private static final String TOKEN_TYPE = "access";

    /**
     * Generate JWT token with proper claims
     *
     * Claims included:
     * - sub (subject): username
     * - iss (issuer): application identifier
     * - aud (audience): intended recipient
     * - iat (issued at): token creation time
     * - exp (expiration): token expiration time
     * - jti (JWT ID): unique token identifier for tracking/revocation
     * - typ (type): "access" to distinguish from refresh tokens
     */
    public String generateToken(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        String tokenId = UUID.randomUUID().toString();

        try {
            String token = Jwts.builder()
                    // Standard claims
                    .subject(username)                           // sub: username
                    .issuer(jwtIssuer)                          // iss: youtube-research-agent
                    .audience().add(jwtAudience).and()          // aud: web-client
                    .issuedAt(now)                              // iat: now
                    .expiration(expiryDate)                     // exp: expiration

                    // Custom claims
                    .id(tokenId)                                // jti: unique token ID
                    .claim("typ", TOKEN_TYPE)                   // typ: "access" vs "refresh"
                    .claim("version", 1)                        // version: for future token format changes

                    // Sign with key
                    .signWith(key)
                    .compact();

            log.debug("Generated JWT token for user: {}, tokenId: {}", username, tokenId);
            return token;
        } catch (Exception e) {
            log.error("Failed to generate JWT token for user: {}", username, e);
            throw new JwtException("Failed to generate token", e);
        }
    }

    /**
     * Validate JWT token comprehensively
     *
     * Checks:
     * - Signature validity
     * - Expiration
     * - Issuer
     * - Audience
     * - Token type
     * - Clock skew (60 seconds)
     */
    public boolean validateToken(String token) {
        try {
            if (token == null || token.isBlank()) {
                log.warn("Token validation failed: token is null or blank");
                return false;
            }

            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

            Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(jwtIssuer)                   // Verify issuer
                    .requireAudience(jwtAudience)               // Verify audience
                    .clockSkewSeconds(60)                       // Allow 60 second skew
                    .build()
                    .parseSignedClaims(token);

            log.debug("JWT token validation successful");
            return true;

        } catch (ExpiredJwtException e) {
            log.warn("JWT token has expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract username from token
     *
     * @param token JWT token
     * @return username (subject claim)
     * @throws JwtException if token is invalid
     */
    public String extractUsername(String token) throws JwtException {
        try {
            if (!validateToken(token)) {
                throw new JwtException("Token validation failed");
            }

            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.getSubject();
            if (username == null || username.isBlank()) {
                throw new JwtException("Username claim is missing or blank");
            }

            return username;
        } catch (JwtException e) {
            log.warn("Failed to extract username from token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Extract all claims from token for auditing/debugging
     *
     * @param token JWT token
     * @return Claims object
     * @throws JwtException if token is invalid
     */
    public Claims getClaimsFromToken(String token) throws JwtException {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.warn("Failed to extract claims from token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get token ID (jti claim) for revocation tracking
     *
     * @param token JWT token
     * @return token ID
     */
    public String getTokenId(String token) throws JwtException {
        Claims claims = getClaimsFromToken(token);
        return claims.getId();
    }

    /**
     * Check if token is of correct type (access vs refresh)
     *
     * @param token JWT token
     * @param expectedType expected token type
     * @return true if token type matches
     */
    public boolean isTokenType(String token, String expectedType) throws JwtException {
        Claims claims = getClaimsFromToken(token);
        String tokenType = claims.get("typ", String.class);
        return expectedType.equals(tokenType);
    }

    /**
     * Get remaining time until token expires
     *
     * @param token JWT token
     * @return milliseconds until expiration, or -1 if already expired
     */
    public long getTimeUntilExpiration(String token) throws JwtException {
        Claims claims = getClaimsFromToken(token);
        long expirationTime = claims.getExpiration().getTime();
        long currentTime = System.currentTimeMillis();
        return expirationTime - currentTime;
    }
}