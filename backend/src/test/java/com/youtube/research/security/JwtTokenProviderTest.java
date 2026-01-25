package com.youtube.research.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class JwtTokenProviderTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void shouldGenerateValidToken() {
        String username = "testuser";
        String token = jwtTokenProvider.generateToken(username);

        assertThat(token).isNotBlank();
        assertThat(token).contains(".");
    }

    @Test
    void shouldExtractUsernameFromToken() {
        String username = "testuser";
        String token = jwtTokenProvider.generateToken(username);
        String extractedUsername = jwtTokenProvider.extractUsername(token);

        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    void shouldValidateToken() {
        String username = "testuser";
        String token = jwtTokenProvider.generateToken(username);
        boolean isValid = jwtTokenProvider.validateToken(token);

        assertThat(isValid).isTrue();
    }
}