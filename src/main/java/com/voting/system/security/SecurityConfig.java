package com.voting.system.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security Configuration.
 * 
 * Simple-English Explanation:
 * 1. CSRF is DISABLED:
 *    CSRF (Cross-Site Request Forgery) attacks exploit ambient browser cookie authentication.
 *    Because this application uses STATELESS REST authentication with JWT tokens sent via
 *    the "Authorization: Bearer <token>" header (and no cookies), CSRF protection is not needed.
 * 2. Sessions are STATELESS:
 *    The server creates no HttpSession. Every incoming request must provide a valid JWT.
 * 3. Role-Based Access Control (RBAC):
 *    - Only ADMIN can create/edit elections, manage candidates, and view participation stats.
 *    - Only VOTER can cast votes.
 *    - Anonymous users can only access public static files, registration, and login.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtTokenProvider tokenProvider,
                          CustomUserDetailsService userDetailsService,
                          PasswordEncoder passwordEncoder,
                          CustomAuthenticationEntryPoint authenticationEntryPoint,
                          CustomAccessDeniedHandler accessDeniedHandler) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(tokenProvider, userDetailsService);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF because authentication uses Bearer tokens in headers, not cookies
                .csrf(csrf -> csrf.disable())
                // Configure CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Allow frames from same origin for H2 Web Console
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                // Stateless session management (no HttpSession created or stored)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Custom 401 and 403 JSON handlers
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                // Endpoint Authorization Rules
                .authorizeHttpRequests(auth -> auth
                        // Public Endpoints: Auth, Static Frontend, OpenAPI/Swagger, H2 Console
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()

                        // Voting Endpoints: Strictly VOTER role only
                        .requestMatchers(HttpMethod.POST, "/api/elections/*/votes").hasRole("VOTER")

                        // Admin Election & Candidate Management: Strictly ADMIN role only
                        .requestMatchers(HttpMethod.POST, "/api/elections").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/elections/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/elections/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/elections/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/elections/*/results-visibility").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/elections/*/participation").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/elections/*/candidates").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/candidates/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/candidates/*").hasRole("ADMIN")

                        // All other /api endpoints require authenticated user (VOTER or ADMIN)
                        .requestMatchers("/api/**").authenticated()

                        // Any other request
                        .anyRequest().permitAll()
                );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
