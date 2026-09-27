package com.warrantypro.auth.dto;

/**
 * 登录 / 刷新响应（与 PC 端 stores/user.ts 的 LoginResult 对齐）。
 */
public record LoginResponse(String accessToken, String refreshToken, UserInfoVO user) {
}
