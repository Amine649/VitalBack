package com.veterinaire.formulaireveterinaire.Config;

import com.veterinaire.formulaireveterinaire.Enums.SubscriptionStatus;
import com.veterinaire.formulaireveterinaire.DAO.SubscriptionRepository;
import com.veterinaire.formulaireveterinaire.DAO.UserRepository;
import com.veterinaire.formulaireveterinaire.entity.Subscription;
import com.veterinaire.formulaireveterinaire.entity.User;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.util.AntPathMatcher;


@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    //**
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    private static final List<String> PERMIT_ALL_ENDPOINTS = Arrays.asList(
            "/api/login", "/api/users/register" ,"/api/reset-password" , "/api/forgot-password-otp" , "/api/reset-password-otp"
    );

    private static final List<String> SKIP_SUBSCRIPTION_CHECK_ENDPOINTS = Arrays.asList(
            "/api/veterinaires/me",
            "/api/veterinaires/all",
            "/api/logout",
            "/api/reset-password",
            "/api/cart",
            "/api/commercial/orders",

            "/api/blogs/type/PROPRIETAIRE",
            "/api/blogs/type/VETERINAIRE",
            "/api/blogs/pdf/**",

            "/api/cart/**"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        logger.debug("Processing request for URI: {}", requestURI);
        logger.debug("Request method: {}", request.getMethod());

        // Skip authentication for permitAll endpoints
        boolean isPermitAll = PERMIT_ALL_ENDPOINTS.stream().anyMatch(requestURI::startsWith);
        if (isPermitAll) {
            logger.debug("Skipping authentication for permitAll endpoint: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        String token = null;
        String email = null;

        // Extract token from cookies
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    try {
                        email = jwtUtil.extractEmail(token);
                    } catch (Exception e) {
                        logger.error("Invalid token: {}", e.getMessage());
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\": \"Invalid token format\"}");
                        return;
                    }
                    break;
                }
            }
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            final String finalEmail = email; // ✅ make it effectively final

            UserDetails userDetails = userDetailsService.loadUserByUsername(finalEmail);

            if (userDetails != null && jwtUtil.validateToken(token, finalEmail)) {
                User user = userRepository.findByEmail(finalEmail)
                        .orElseThrow(() -> new RuntimeException("User not found: " + finalEmail));

                //**
                boolean skipSubscriptionCheck = SKIP_SUBSCRIPTION_CHECK_ENDPOINTS.stream()
                        .anyMatch(pattern -> pathMatcher.match(pattern, requestURI));


//                boolean skipSubscriptionCheck = SKIP_SUBSCRIPTION_CHECK_ENDPOINTS.stream()
//                        .anyMatch(requestURI::startsWith);

                if (!user.isAdmin() && !user.isCommercial() && !skipSubscriptionCheck) {

                    Subscription subscription = subscriptionRepository.findByUserEmail(finalEmail)
                            .orElseThrow(() -> new RuntimeException("No subscription found for user: " + finalEmail));

                    if (subscription.getEndDate().isBefore(LocalDateTime.now())) {
                        logger.warn("Subscription expired for user: {}", finalEmail);
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\": \"Subscription expired\"}");
                        return;
                    }
                } else {
                    logger.debug("Skipping subscription check for endpoint {} or admin user", requestURI);
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("Authenticated user: {}", finalEmail);
            } else {
                logger.warn("Invalid token or user details for email: {}", finalEmail);
            }
        }

        filterChain.doFilter(request, response);
    }
}