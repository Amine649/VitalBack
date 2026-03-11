package com.veterinaire.formulaireveterinaire.Config;

import com.veterinaire.formulaireveterinaire.DAO.SubscriptionRepository;
import com.veterinaire.formulaireveterinaire.DAO.UserRepository;
import com.veterinaire.formulaireveterinaire.serviceimpl.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {    
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService,
                          SubscriptionRepository subscriptionRepository, UserRepository userRepository
                          ,CorsConfigurationSource corsConfigurationSource) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    // Configure security filter chain to define which endpoints are public and which require authentication
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Disable frame options)
                .headers(headers -> headers
                        .frameOptions(fo -> fo
                                .disable()
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/login", "/api/logout" ,"/api/users/register").permitAll()
                        .requestMatchers("/api/reset-password").authenticated()
                        .requestMatchers("/api/forgot-password-otp").permitAll()
                        .requestMatchers("/api/reset-password-otp").permitAll()
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/products/all").permitAll()
                        .requestMatchers("/api/products/*/variants").permitAll()
                        .requestMatchers("/api/cart/items/*").permitAll()


                        .requestMatchers("/api/cabinets/all").permitAll()
                        .requestMatchers("/api/boutiques/all").permitAll()
                        .requestMatchers("/api/products/add").permitAll()
                        .requestMatchers("api/cart/commercial-checkout").permitAll()
                        .requestMatchers("/api/veterinaires/update").permitAll()


                        .requestMatchers("/api/blogs/all").permitAll()
                        .requestMatchers("/api/blogs/type/**").permitAll()

                        .requestMatchers("/api/blogs/type/PROPRIETAIRE").permitAll()
                        .requestMatchers("/api/blogs/type/VETERINAIRE").permitAll()

                        .requestMatchers("/api/blogs/pet/CAT").permitAll()
                        .requestMatchers("/api/blogs/pet/DOG").permitAll()

                        .requestMatchers("/api/blogs/pdf/**").permitAll()

                        .requestMatchers("/api/blogs/add").permitAll()
                        .requestMatchers("/api/blogs/add").permitAll()


                        //
                        .requestMatchers("/api/veterinaires").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Authentication failed: " + authException.getMessage() + "\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Access denied: Insufficient permissions\"}");
                        })
                );



        http.addFilterBefore(
                new JwtAuthenticationFilter(jwtUtil, userDetailsService, subscriptionRepository, userRepository),
                UsernamePasswordAuthenticationFilter.class
        );



        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}