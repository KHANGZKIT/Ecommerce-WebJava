package com.ecommerce.orderservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class OrderSecurityConfig {

    @Bean
    SecurityFilterChain orderSecurityFilterChain(HttpSecurity http, OrderJwtVerifier verifier) throws Exception {
        AuthenticationEntryPoint unauthorized = (request, response, exception) -> {
            response.setStatus(401);
            response.setContentType("application/json");
            response.setHeader("WWW-Authenticate", "Bearer");
            response.getWriter().write("{\"status\":401,\"message\":\"Valid bearer authentication required\"}");
        };

        AccessDeniedHandler accessDenied = (request, response, exception) -> {
            response.setStatus(403);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":403,\"message\":\"Access denied\"}");
        };

        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(accessDenied))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**",
                                "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml",
                                "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers("/api/v1/orders/checkout", "/api/v1/checkout", "/api/v1/cart", "/api/v1/cart/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/orders/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/*/cancel").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(new OrderJwtAuthenticationFilter(verifier, unauthorized),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
