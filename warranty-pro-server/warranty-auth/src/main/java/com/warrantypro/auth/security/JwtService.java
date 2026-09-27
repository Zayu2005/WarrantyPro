package com.warrantypro.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JWT 签发与解析（jjwt 0.12，HS256）。access 携带角色；refresh 仅用于换发。
 */
@Component
@RequiredArgsConstructor
public class JwtService {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final JwtProperties properties;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccess(Long userId, List<String> roles) {
        return build(userId, TYPE_ACCESS, roles, Instant.now().plusSeconds(properties.getAccessExpireMinutes() * 60L));
    }

    public String createRefresh(Long userId) {
        return build(userId, TYPE_REFRESH, null, Instant.now().plusSeconds(properties.getRefreshExpireDays() * 86400L));
    }

    private String build(Long userId, String type, List<String> roles, Instant expiresAt) {
        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("typ", type)
                .issuedAt(new Date())
                .expiration(Date.from(expiresAt))
                .signWith(key());
        if (roles != null) {
            builder.claim("roles", roles);
        }
        return builder.compact();
    }

    /**
     * 解析并验签。失败抛 JwtException（含过期），由调用方决定如何处理。
     */
    public TokenClaims parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key()).build()
                .parseSignedClaims(token).getPayload();
        Long userId = Long.valueOf(claims.getSubject());
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        return new TokenClaims(userId, roles, claims.get("typ", String.class),
                claims.getId(), claims.getExpiration().toInstant());
    }

    /**
     * 解析失败（签名不合法 / 过期 / 格式错误）时返回 null，不抛异常 —— 供过滤器静默跳过。
     */
    public TokenClaims tryParse(String token) {
        try {
            return parse(token);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public record TokenClaims(Long userId, List<String> roles, String type, String jti, Instant expiresAt) {
    }
}
