package com.warrantypro.auth.web;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登出令牌黑名单（jti → 过期时间）。
 *
 * <p>演示阶段用进程内 Map（单实例部署足够）；生产/多实例部署时替换为 Redis 实现
 * （key: wp:auth:blacklist:{jti}，TTL = 令牌剩余有效期），接口不变。</p>
 */
@Component
public class TokenBlacklist {

    private static final int MAX_SIZE = 10_000;

    private final Map<String, Instant> blacklisted = new ConcurrentHashMap<>();

    public void add(String jti, Instant expiresAt) {
        if (blacklisted.size() >= MAX_SIZE) {
            cleanUp();
        }
        blacklisted.put(jti, expiresAt);
    }

    public boolean contains(String jti) {
        return blacklisted.containsKey(jti);
    }

    private void cleanUp() {
        Instant now = Instant.now();
        blacklisted.entrySet().removeIf(e -> e.getValue().isBefore(now));
    }
}
