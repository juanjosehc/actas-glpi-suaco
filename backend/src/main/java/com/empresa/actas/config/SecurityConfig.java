package com.empresa.actas.config;

import com.empresa.actas.security.CustomUserDetailsService;
import com.empresa.actas.security.JwtAuthenticationFilter;
import com.empresa.actas.security.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final RateLimitFilter rateLimitFilter;

    // SEC-015: Swagger/OpenAPI publicos solo en desarrollo. En produccion se
    // desactiva con APP_DOCUMENTACION_PUBLICA=false (o app.documentacion.publica);
    // con false, /swagger-ui y /v3/api-docs exigen JWT (anyRequest().authenticated()).
    @Value("${app.documentacion.publica:true}")
    private boolean documentacionPublica;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/auth/login",
                            "/auth/register",
                            "/equipo/**",
                            "/generar-acta",
                            "/generar-devolucion",
                            "/generar-formateo-seguro",
                            "/firma/**",
                            "/uploads/**",
                            "/health",
                            "/error"
                    ).permitAll();
                    if (documentacionPublica) {
                        auth.requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                // Rate limiting de /auth/login y /auth/register (SEC-004) se
                // aplica antes que la autenticacion (UsernamePasswordAuthenticationFilter).
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)
                // Endurecimiento de headers (SEC-007). X-Content-Type-Options y
                // Cache-Control ya los emite Spring por defecto (nosniff, no-store).
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())
                        // CSP compatible con la app actual: Swagger UI se sirve
                        // del mismo origen con scripts inline en su bootstrap.
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                "script-src 'self' 'unsafe-inline'; " +
                                "style-src 'self' 'unsafe-inline'; " +
                                "img-src 'self' data:; " +
                                "font-src 'self'; " +
                                "connect-src 'self'; " +
                                "frame-ancestors 'self'; " +
                                "base-uri 'self'; " +
                                "form-action 'self'"))
                        // No enviar Referer: los enlaces de firma llevan ?token=
                        // en la URL y no debe filtrarse a terceros.
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        // Spring solo lo emite sobre HTTPS (SecureRequestMatcher).
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true))
                        // Bloquea sensores/UBI no usados por la app. Camera queda
                        // libre: el portal de firma captura foto del firmante.
                        // (Ultima en la cadena: su retorno no encadena en 6.4.)
                        .permissionsPolicy(permissions -> permissions.policy(
                                "geolocation=(), microphone=(), usb=(), payment=()")));

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://127.0.0.1", "http://localhost",
                "http://127.0.0.1:5500", "http://localhost:5500",
                "http://127.0.0.1:80", "http://localhost:80",
                "http://127.0.0.1:8080", "http://localhost:8080",
                "http://127.0.0.1:8001", "http://localhost:8001"));
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
