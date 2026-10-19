package com.jitong.projectflow.auth.security;

import com.jitong.projectflow.system.service.CurrentUserPermissionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenService jwtTokenService;
    private final CurrentUserPermissionService currentUserPermissionService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, CurrentUserPermissionService currentUserPermissionService) {
        this.jwtTokenService = jwtTokenService;
        this.currentUserPermissionService = currentUserPermissionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                Long userId = jwtTokenService.parseUserId(token);
                List<SimpleGrantedAuthority> authorities = currentUserPermissionService.getAuthoritiesByUserId(userId).stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();
                String deptName = currentUserPermissionService.getDeptNameByUserId(userId);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                CurrentUserContext.set(userId);
                CurrentUserContext.setDeptName(deptName);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            CurrentUserContext.clear();
            SecurityContextHolder.clearContext();
            log.debug("JWT token rejected for {}: {}", request.getRequestURI(), e.getMessage());
        }
        try {
            chain.doFilter(request, response);
        } finally {
            CurrentUserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
