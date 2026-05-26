package com.youtube.research.config;

import com.youtube.research.security.JwtAuthenticationEntryPoint;
import com.youtube.research.security.JwtAuthenticationFilter;
import com.youtube.research.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(csrfSpec -> csrfSpec.disable())
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .pathMatchers("/api/users/register").permitAll()
                        .pathMatchers("/api/users/login").permitAll()
                        .pathMatchers("/api/users/**").authenticated()
                        .pathMatchers("/api/conversations/**").authenticated()
                        .pathMatchers("/api/youtube/**").authenticated()
                        .anyExchange().authenticated()
                )
                // Place the JWT filter BEFORE the standard authentication mechanisms
                // This allows it to run and potentially set an authentication object
                // before the authorization check (.authenticated()) occurs.
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), SecurityWebFiltersOrder.AUTHENTICATION);

        return http.build();
    }
}