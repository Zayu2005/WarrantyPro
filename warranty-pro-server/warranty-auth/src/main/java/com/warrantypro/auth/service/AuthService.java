package com.warrantypro.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.warrantypro.auth.dto.LoginRequest;
import com.warrantypro.auth.dto.LoginResponse;
import com.warrantypro.auth.dto.UserInfoVO;
import com.warrantypro.auth.security.JwtService;
import com.warrantypro.auth.web.TokenBlacklist;
import com.warrantypro.common.exception.BizException;
import com.warrantypro.common.exception.ErrorCode;
import com.warrantypro.common.security.LoginUser;
import com.warrantypro.user.entity.SysUser;
import com.warrantypro.user.mapper.SysUserMapper;
import com.warrantypro.user.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证服务：密码登录、令牌刷新、登出（黑名单）、当前用户（docs/07 §2.1）。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklist tokenBlacklist;

    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.username()));
        if (user == null || user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号已停用，请联系管理员");
        }
        return issueTokens(user, true);
    }

    public LoginResponse refresh(String refreshToken) {
        JwtService.TokenClaims claims = jwtService.parse(refreshToken);
        if (!JwtService.TYPE_REFRESH.equals(claims.type())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "非法的刷新令牌");
        }
        SysUser user = sysUserMapper.selectById(claims.userId());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "账号不可用");
        }
        return issueTokens(user, true);
    }

    public void logout(String accessToken) {
        JwtService.TokenClaims claims = jwtService.tryParse(accessToken);
        if (claims != null && claims.jti() != null) {
            tokenBlacklist.add(claims.jti(), claims.expiresAt());
        }
    }

    public UserInfoVO me(LoginUser user) {
        // JWT 只携带 ID 与角色，用户名/姓名回库取最新值
        SysUser dbUser = sysUserMapper.selectById(user.userId());
        if (dbUser == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        return new UserInfoVO(dbUser.getId(), dbUser.getUsername(), dbUser.getPhone(),
                dbUser.getRealName(), user.roles());
    }

    private LoginResponse issueTokens(SysUser user, boolean rotate) {
        List<String> roles = sysUserRoleMapper.selectRoleCodes(user.getId());
        String access = jwtService.createAccess(user.getId(), roles);
        String refresh = jwtService.createRefresh(user.getId());
        if (rotate) {
            user.setLastLoginAt(LocalDateTime.now());
            sysUserMapper.updateById(user);
        }
        return new LoginResponse(access, refresh, new UserInfoVO(user.getId(), user.getUsername(),
                user.getPhone(), user.getRealName(), roles));
    }
}
