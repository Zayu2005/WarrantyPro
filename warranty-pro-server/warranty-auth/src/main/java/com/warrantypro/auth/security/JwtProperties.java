package com.warrantypro.auth.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** JWT 配置（warranty.jwt.*）。生产环境必须通过环境变量覆盖 secret。 */
@Data
@Component
@ConfigurationProperties(prefix = "warranty.jwt")
public class JwtProperties {

    /** HS256 签名密钥，长度 ≥ 32 字节 */
    private String secret = "warrantypro-dev-jwt-secret-please-change-in-production";

    /** Access Token 有效期（分钟） */
    private int accessExpireMinutes = 120;

    /** Refresh Token 有效期（天） */
    private int refreshExpireDays = 7;
}
