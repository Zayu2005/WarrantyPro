package com.warrantypro.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.warrantypro.common.exception.BizException;
import com.warrantypro.common.exception.ErrorCode;
import com.warrantypro.common.result.Result;
import com.warrantypro.user.entity.SysMenu;
import com.warrantypro.user.mapper.SysMenuMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/menus")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMenuController {

    private final SysMenuMapper menuMapper;

    @GetMapping
    public Result<List<SysMenu>> tree() {
        List<SysMenu> nodes = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSortOrder).orderByAsc(SysMenu::getId));
        Map<Long, SysMenu> byId = new HashMap<>();
        nodes.forEach(menu -> {
            menu.setChildren(new ArrayList<>());
            byId.put(menu.getId(), menu);
        });
        List<SysMenu> roots = new ArrayList<>();
        for (SysMenu menu : nodes) {
            SysMenu parent = byId.get(menu.getParentId());
            if (parent == null) roots.add(menu);
            else parent.getChildren().add(menu);
        }
        sortTree(roots);
        return Result.ok(roots);
    }

    @PostMapping
    public Result<SysMenu> create(@Valid @RequestBody MenuRequest body) {
        validate(body, null);
        SysMenu menu = toEntity(body);
        menuMapper.insert(menu);
        return Result.ok(menu);
    }

    @PutMapping("/{id}")
    public Result<SysMenu> update(@PathVariable Long id, @Valid @RequestBody MenuRequest body) {
        SysMenu existing = requireMenu(id);
        validate(body, id);
        apply(existing, body);
        menuMapper.updateById(existing);
        return Result.ok(existing);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result<Void> delete(@PathVariable Long id) {
        requireMenu(id);
        if (menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id)) > 0) {
            throw new BizException(ErrorCode.CONFLICT, "请先删除或移动该菜单下的子菜单");
        }
        menuMapper.deleteById(id);
        return Result.ok();
    }

    private void validate(MenuRequest body, Long currentId) {
        if (!"DIR".equals(body.getType()) && !"MENU".equals(body.getType())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "菜单类型仅支持目录或页面");
        }
        long parentId = body.getParentId() == null ? 0L : body.getParentId();
        if (parentId != 0) {
            SysMenu parent = requireMenu(parentId);
            if ("DIR".equals(body.getType()) && !"DIR".equals(parent.getType())) {
                throw new BizException(ErrorCode.PARAM_INVALID, "目录只能放在顶级目录或其他目录下");
            }
            if ("MENU".equals(body.getType()) && !"DIR".equals(parent.getType())) {
                throw new BizException(ErrorCode.PARAM_INVALID, "页面菜单必须归属一个目录");
            }
            if (currentId != null) {
                Long ancestorId = parentId;
                while (ancestorId != null && ancestorId != 0) {
                    if (ancestorId.equals(currentId)) {
                        throw new BizException(ErrorCode.PARAM_INVALID, "不能将菜单移动到自身或下级目录中");
                    }
                    SysMenu ancestor = menuMapper.selectById(ancestorId);
                    ancestorId = ancestor == null ? 0L : ancestor.getParentId();
                }
            }
        }
        if ("DIR".equals(body.getType()) && body.getPath() != null && !body.getPath().isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "目录不需要配置页面路径");
        }
        if ("MENU".equals(body.getType())) {
            if (body.getPath() == null || body.getPath().isBlank()) {
                throw new BizException(ErrorCode.PARAM_INVALID, "页面菜单必须配置页面路径");
            }
            boolean duplicate = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getPath, body.getPath())
                    .ne(currentId != null, SysMenu::getId, currentId)) > 0;
            if (duplicate) throw new BizException(ErrorCode.CONFLICT, "该页面路径已配置");
        }
    }

    private SysMenu requireMenu(Long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) throw new BizException(ErrorCode.NOT_FOUND, "菜单不存在");
        return menu;
    }

    private SysMenu toEntity(MenuRequest body) {
        SysMenu menu = new SysMenu();
        apply(menu, body);
        return menu;
    }

    private void apply(SysMenu menu, MenuRequest body) {
        menu.setParentId(body.getParentId() == null ? 0L : body.getParentId());
        menu.setName(body.getName().trim());
        menu.setPath("DIR".equals(body.getType()) ? "" : body.getPath().trim());
        menu.setIcon(body.getIcon() == null ? "" : body.getIcon().trim());
        menu.setType(body.getType());
        menu.setSortOrder(body.getSortOrder() == null ? 0 : body.getSortOrder());
        menu.setVisible(body.getVisible() == null ? 1 : body.getVisible());
        menu.setStatus(body.getStatus() == null ? 1 : body.getStatus());
    }

    private void sortTree(List<SysMenu> nodes) {
        nodes.sort(Comparator.comparing(SysMenu::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SysMenu::getId));
        nodes.forEach(node -> sortTree(node.getChildren()));
    }

    @Data
    public static class MenuRequest {
        @NotNull private Long parentId;
        @NotBlank @Size(max = 50) private String name;
        @Size(max = 100) @Pattern(regexp = "^$|^/[a-zA-Z0-9/_-]+$", message = "页面路径格式无效")
        private String path;
        @Size(max = 40) private String icon;
        @NotBlank private String type;
        @NotNull @Min(0) @Max(999) private Integer sortOrder;
        @NotNull @Min(0) @Max(1) private Integer visible;
        @NotNull @Min(0) @Max(1) private Integer status;
    }
}
