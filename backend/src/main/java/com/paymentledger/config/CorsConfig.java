package com.paymentledger.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * CORS (Cross-Origin Resource Sharing) configuration.
 * 
 * Enables the frontend (running on localhost:5173) to make requests to the backend (localhost:8080).
 * In production, adjust the allowed origins to match your domain.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow requests from the frontend development server and production domain
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173",      // Local React dev server
            "http://localhost:3000",      // Alternative local dev server
            "http://127.0.0.1:5173",      // Localhost alternative
            "http://127.0.0.1:3000"       // Localhost alternative
        ));
        
        // Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Allow the Authorization header and other common headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Expose headers if needed by the frontend
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Total-Count"));
        
        // Cache preflight requests for 1 hour
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
