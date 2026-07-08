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

/**
 * 仅在 projectflow.security.enabled=false 时激活。
 * 无需 Token 即放行所有请求，自动以 devUserId 身份注入权限，
 * 也支持携带有效 Bearer Token 时切换到真实用户。
 */
public class DevBypassFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(DevBypassFilter.class);

    private final JwtTokenService jwtTokenService;
    private final CurrentUserPermissionService currentUserPermissionService;
    private final long devUserId;

    public DevBypassFilter(JwtTokenService jwtTokenService,
                           CurrentUserPermissionService currentUserPermissionService,
                           long devUserId) {
        this.jwtTokenService = jwtTokenService;
        this.currentUserPermissionService = currentUserPermissionService;
        this.devUserId = devUserId;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            Long userId = devUserId;
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                try {
                    userId = jwtTokenService.parseUserId(header.substring(7));
                } catch (Exception e) {
                    log.debug("Dev mode: invalid token, falling back to devUserId={}", devUserId);
                }
            }
            CurrentUserContext.set(userId);
            List<SimpleGrantedAuthority> authorities = currentUserPermissionService.getPermissionsByUserId(userId).stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.warn("Dev bypass filter error for {}: {}", request.getRequestURI(), e.getMessage());
        }
        try {
            chain.doFilter(request, response);
        } finally {
            CurrentUserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
