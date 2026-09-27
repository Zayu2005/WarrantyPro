package com.warrantypro.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** 刷新令牌请求体。 */
public record RefreshBody(
        @NotBlank
        String refreshToken
) {
}
