package com.warrantypro.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** 登录请求（docs/07 §2.1；验证码登录随通知中心实现后接入）。 */
public record LoginRequest(
        @NotBlank
        String phone,

        @NotBlank
        String password
) {
}
