package com.warrantypro.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 登录请求：用户名 + 密码（验证码登录随通知中心实现后接入）。 */
public record LoginRequest(
        @NotBlank
        @Size(max = 50)
        String username,

        @NotBlank
        String password
) {
}
