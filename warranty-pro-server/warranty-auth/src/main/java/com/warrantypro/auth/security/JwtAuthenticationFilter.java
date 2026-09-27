package com.warrantypro.auth.security;

import com.warrantypro.auth.web.TokenBlacklist;
import com.warrantypro.common.security.LoginUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：解析 Bearer Token → 构造 LoginUser → 写入 SecurityContext。
 * 解析失败 / 无 token 时静默放行，由授权规则与 EntryPoint 兜底。
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklist tokenBlacklist;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            JwtService.TokenClaims claims = jwtService.tryParse(header.substring(7));
            if (claims != null && JwtService.TYPE_ACCESS.equals(claims.type())
                    && !tokenBlacklist.contains(claims.jti())) {
                LoginUser principal = new LoginUser(claims.userId(), null, null, claims.roles());
                List<SimpleGrantedAuthority> authorities = claims.roles().stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                        .toList();
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal, null, authorities));
            }
        }
        chain.doFilter(request, response);
    }
}
