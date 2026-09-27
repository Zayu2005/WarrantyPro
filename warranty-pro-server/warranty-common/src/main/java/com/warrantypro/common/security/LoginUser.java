package com.warrantypro.common.security;

import java.util.List;

/**
 * 登录用户主体：由认证模块的 JWT 过滤器构造，放入 SecurityContext，
 * 各业务模块控制器通过 @AuthenticationPrincipal 直接获取（避免模块间依赖 auth）。
 */
public record LoginUser(Long userId, String phone, String realName, List<String> roles) {

    public boolean hasRole(String roleCode) {
        return roles != null && roles.contains(roleCode);
    }
}
