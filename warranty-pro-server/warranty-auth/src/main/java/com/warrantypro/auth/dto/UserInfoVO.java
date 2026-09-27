package com.warrantypro.auth.dto;

import java.util.List;

/** 用户信息 VO（与 PC 端 LoginResult.user 结构对齐）。 */
public record UserInfoVO(Long id, String phone, String realName, List<String> roles) {
}
