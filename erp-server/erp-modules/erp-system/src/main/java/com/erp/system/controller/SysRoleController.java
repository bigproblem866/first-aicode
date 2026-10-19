package com.erp.system.controller;

import com.erp.common.core.domain.PageResult;
import com.erp.common.core.domain.R;
import com.erp.common.core.utils.StringUtils;
import com.erp.system.domain.SysRole;
import com.erp.system.domain.dto.SysRoleQuery;
import com.erp.system.service.ISysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理接口。
 * <p>
 * 权限串 {@code system:role:xxx} 必须与菜单种子数据的 perms 列逐字一致。
 */
@Tag(name = "角色管理", description = "系统角色的增删改查与权限分配")
@RestController
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final ISysRoleService sysRoleService;

    @Operation(summary = "角色列表", description = "分页返回角色")
    @PreAuthorize("@ss.hasPermi('system:role:list')")
    @GetMapping("/list")
    public R<PageResult<SysRole>> list(SysRoleQuery query) {
        return R.ok(sysRoleService.selectRolePage(query));
    }

    @Operation(summary = "角色详情", description = "返回角色及其已授权的菜单 ID，供编辑页回显")
    @PreAuthorize("@ss.hasPermi('system:role:query')")
    @GetMapping("/{roleId}")
    public R<SysRole> getInfo(@PathVariable("roleId") Long roleId) {
        return R.ok(sysRoleService.selectRoleById(roleId));
    }

    @Operation(summary = "新增角色", description = "roleKey 唯一，同时写入角色-菜单关联")
    @PreAuthorize("@ss.hasPermi('system:role:add')")
    @PostMapping
    public R<Void> add(@Valid @RequestBody SysRole role) {
        sysRoleService.insertRole(role);
        return R.ok();
    }

    @Operation(summary = "修改角色", description = "不允许修改超级管理员角色的 roleKey")
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @PutMapping
    public R<Void> edit(@Valid @RequestBody SysRole role) {
        sysRoleService.updateRole(role);
        return R.ok();
    }

    @Operation(summary = "删除角色", description = "ids 为逗号分隔字符串；禁止删除超级管理员角色及已分配的角色")
    @PreAuthorize("@ss.hasPermi('system:role:remove')")
    @DeleteMapping("/{roleIds}")
    public R<Void> remove(@PathVariable("roleIds") String roleIds) {
        sysRoleService.deleteRoleByIds(StringUtils.splitToLong(roleIds));
        return R.ok();
    }

    @Operation(summary = "角色下拉", description = "返回全部正常状态角色（不分页），给用户分配角色用")
    @GetMapping("/optionselect")
    public R<List<SysRole>> optionselect() {
        return R.ok(sysRoleService.selectRoleAll());
    }
}
