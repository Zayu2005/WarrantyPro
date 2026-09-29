package com.warrantypro.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 登录请求：用户名 + 密码 + 一次性滑块挑战。 */
public record LoginRequest(
        @NotBlank
        @Size(max = 50)
        String username,

        @NotBlank
        String password,

        @NotBlank
        String challengeId
) {
}
