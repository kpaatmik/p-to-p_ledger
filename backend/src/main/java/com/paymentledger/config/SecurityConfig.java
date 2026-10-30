
package com.paymentledger.config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.paymentledger.security.JwtAuthFilter;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    public SecurityConfig(
            JwtAuthFilter jwtAuthFilter,
            UserDetailsService userDetailsService,
            ObjectMapper objectMapper) {

        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {

        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)
            throws Exception {

        http
            // Stateless REST API
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    HttpMethod.POST,
                    "/auth/register",
                    "/auth/login"
                ).permitAll()

                .requestMatchers("/admin/**").hasRole("ADMIN")

                .anyRequest().authenticated()
            )

            // Authentication and authorization error handlers
            .exceptionHandling(ex -> ex

                .authenticationEntryPoint(
                    (request, response, authException) -> {

                        response.setStatus(
                            HttpServletResponse.SC_UNAUTHORIZED
                        );

                        response.setContentType(
                            MediaType.APPLICATION_JSON_VALUE
                        );

                        Map<String, Object> body = Map.of(
                            "status", 401,
                            "error", "Unauthorized",
                            "message", "Authentication required",
                            "path", request.getRequestURI()
                        );

                        objectMapper.writeValue(
                            response.getWriter(), body
                        );
                    }
                )

                .accessDeniedHandler(
                    (request, response, accessDeniedException) -> {

                        response.setStatus(
                            HttpServletResponse.SC_FORBIDDEN
                        );

                        response.setContentType(
                            MediaType.APPLICATION_JSON_VALUE
                        );

                        Map<String, Object> body = Map.of(
                            "status", 403,
                            "error", "Forbidden",
                            "message", "Access denied",
                            "path", request.getRequestURI()
                        );

                        objectMapper.writeValue(
                            response.getWriter(), body
                        );
                    }
                )
            )

            .authenticationProvider(authenticationProvider())

            .addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}
