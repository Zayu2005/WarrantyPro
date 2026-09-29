package com.warrantypro.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warrantypro.common.result.PageResult;
import com.warrantypro.common.result.Result;
import com.warrantypro.user.entity.SysRole;
import com.warrantypro.user.entity.SysUser;
import com.warrantypro.user.mapper.SysRoleMapper;
import com.warrantypro.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/** Read-only directory data used by the operations console. */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AdminDirectoryController {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    @GetMapping("/users")
    public Result<PageResult<UserRow>> users(@RequestParam(required = false) String keyword,
                                             @RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(userPage(keyword, false, page, pageSize));
    }

    @GetMapping("/owners")
    public Result<PageResult<UserRow>> owners(@RequestParam(required = false) String keyword,
                                              @RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(userPage(keyword, true, page, pageSize));
    }

    @GetMapping("/roles")
    public Result<List<SysRole>> roles() {
        return Result.ok(roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId)));
    }

    private PageResult<UserRow> userPage(String keyword, boolean owners, long page, long pageSize) {
        String trimmed = keyword == null ? "" : keyword.trim();
        var query = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeleted, 0)
                .and(!trimmed.isEmpty(), q -> q.like(SysUser::getUsername, trimmed)
                        .or().like(SysUser::getRealName, trimmed)
                        .or().like(SysUser::getPhone, trimmed))
                .orderByDesc(SysUser::getId);
        if (owners) {
            query.inSql(SysUser::getId,
                    "SELECT ur.user_id FROM sys_user_role ur JOIN sys_role r ON r.id = ur.role_id WHERE r.code = 'OWNER'");
        }
        long safePage = Math.max(1, page);
        long safePageSize = Math.min(100, Math.max(1, pageSize));
        Page<SysUser> result = userMapper.selectPage(new Page<>(safePage, safePageSize), query);
        List<UserRow> rows = result.getRecords().stream().map(u -> new UserRow(
                u.getId(), u.getUsername(), u.getRealName(), u.getPhone(), u.getStatus(),
                u.getLastLoginAt(), u.getCreatedAt())).toList();
        return PageResult.of(rows, result.getTotal(), safePage, safePageSize);
    }

    public record UserRow(Long id, String username, String realName, String phone, Integer status,
                          LocalDateTime lastLoginAt, LocalDateTime createdAt) {}
}
