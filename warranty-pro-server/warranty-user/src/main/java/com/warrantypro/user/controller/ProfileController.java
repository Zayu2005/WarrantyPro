package com.warrantypro.user.controller;

import com.warrantypro.common.exception.BizException;
import com.warrantypro.common.exception.ErrorCode;
import com.warrantypro.common.result.Result;
import com.warrantypro.common.security.LoginUser;
import com.warrantypro.user.entity.SysUser;
import com.warrantypro.user.mapper.SysUserMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人资料（docs/02 用户模块）：当前登录用户修改姓名 / 手机号 / 密码。
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class ProfileController {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    /** 修改基本资料（姓名 / 手机号）。 */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> updateProfile(@AuthenticationPrincipal LoginUser user,
                                      @Valid @RequestBody UpdateProfileRequest body) {
        SysUser dbUser = requireUser(user.userId());
        dbUser.setRealName(body.getRealName().trim());
        // 手机号为唯一列：空串转 null，避免多人填空串时冲突
        dbUser.setPhone(body.getPhone() == null || body.getPhone().isBlank() ? null : body.getPhone().trim());
        sysUserMapper.updateById(dbUser);
        return Result.ok();
    }

    /** 修改密码（需验证原密码）。 */
    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> changePassword(@AuthenticationPrincipal LoginUser user,
                                       @Valid @RequestBody ChangePasswordRequest body) {
        SysUser dbUser = requireUser(user.userId());
        if (dbUser.getPasswordHash() == null
                || !passwordEncoder.matches(body.getOldPassword(), dbUser.getPasswordHash())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "原密码不正确");
        }
        dbUser.setPasswordHash(passwordEncoder.encode(body.getNewPassword()));
        sysUserMapper.updateById(dbUser);
        return Result.ok();
    }

    private SysUser requireUser(Long userId) {
        SysUser dbUser = sysUserMapper.selectById(userId);
        if (dbUser == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        return dbUser;
    }

    @Data
    public static class UpdateProfileRequest {
        @NotBlank(message = "姓名不能为空")
        @Size(max = 50, message = "姓名过长")
        private String realName;
        @Size(max = 20, message = "手机号格式不正确")
        private String phone;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "原密码不能为空")
        private String oldPassword;
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 32, message = "新密码长度需在 6~32 位之间")
        private String newPassword;
    }
}
