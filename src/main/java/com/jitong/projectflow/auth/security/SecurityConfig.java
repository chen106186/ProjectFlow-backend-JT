package com.jitong.projectflow.auth.security;

import com.jitong.projectflow.system.service.CurrentUserPermissionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtTokenService jwtTokenService;
    private final CurrentUserPermissionService currentUserPermissionService;
    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    public SecurityConfig(JwtTokenService jwtTokenService,
                          CurrentUserPermissionService currentUserPermissionService,
                          SecurityErrorResponseWriter securityErrorResponseWriter) {
        this.jwtTokenService = jwtTokenService;
        this.currentUserPermissionService = currentUserPermissionService;
        this.securityErrorResponseWriter = securityErrorResponseWriter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                securityErrorResponseWriter.write(response, 401, "Unauthorized"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                securityErrorResponseWriter.write(response, 403, "Forbidden")))
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenService, currentUserPermissionService), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
