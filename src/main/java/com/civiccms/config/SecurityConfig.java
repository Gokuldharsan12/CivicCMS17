package com.civiccms.config;

import com.civiccms.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    // @Lazy breaks the circular dependency:
    // SecurityConfig -> JwtAuthFilter -> AuthService -> SecurityConfig (PasswordEncoder)
    public SecurityConfig(@Lazy JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public static resources
                .requestMatchers(
                    "/", "/index.html", "/login.html", "/register.html",
                    "/submit.html", "/track.html", "/rate.html",
                    "/history.html", "/chatbot.html", "/feedback.html", "/feedback-wall.html", "/api-test.html",
                    "/admin/**", "/dept/**",
                    "/css/**", "/js/**", "/images/**", "/uploads/**", "/favicon.ico"
                ).permitAll()
                // Public API endpoints
                .requestMatchers(
                    "/api/v1/auth/**",
                    "/api/v1/sse/**",
                    "/api/v1/analytics/**",
                    "/api/v1/ratings/**",
                    "/api/v1/departments/**",
                    "/api/ai/**",
                    "/api/heatmap",
                    "/api/complaints/stats"
                ).permitAll()
                // User-specific complaint endpoints — MUST be authenticated so users only see their own data
                .requestMatchers("/api/v1/complaints/mine").authenticated()
                .requestMatchers("/api/v1/complaints/track/**").authenticated()
                // Admin bulk listing also requires auth
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/complaints").authenticated()
                // Remaining complaint endpoints (submit, dept queries, status updates) stay open
                .requestMatchers("/api/v1/complaints/**").permitAll()
                // Content API — GET is public (pages load content without token)
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/content", "/api/content/all").permitAll()
                // Content write endpoints require authentication (admin only in practice)
                .requestMatchers("/api/content/**").authenticated()
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of("*"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
