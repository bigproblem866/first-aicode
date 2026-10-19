package com.erp.system.controller;

import com.erp.common.core.domain.PageResult;
import com.erp.common.core.domain.R;
import com.erp.common.core.utils.StringUtils;
import com.erp.system.domain.SysUser;
import com.erp.system.domain.dto.ResetPwdBody;
import com.erp.system.domain.dto.SysUserQuery;
import com.erp.system.service.ISysUserService;
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

/**
 * 用户管理接口。
 * <p>
 * 权限串 {@code system:user:xxx} 必须与 {@code R__seed_system_menu.sql} 里的 perms 列逐字一致，
 * 写错一个字母的现象是"按钮看得见，点下去 403"。
 * <p>
 * 本层只做参数绑定 + 调用 Service + 包 {@code R}，不写业务逻辑、不注入 Mapper。
 */
@Tag(name = "用户管理", description = "系统用户的增删改查与密码重置")
@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final ISysUserService sysUserService;

    @Operation(summary = "用户列表", description = "分页返回用户，联表带出部门名称")
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/list")
    public R<PageResult<SysUser>> list(SysUserQuery query) {
        return R.ok(sysUserService.selectUserPage(query));
    }

    @Operation(summary = "用户详情", description = "返回用户及其角色 ID、角色名称，供编辑页回显")
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping("/{userId}")
    public R<SysUser> getInfo(@PathVariable("userId") Long userId) {
        return R.ok(sysUserService.selectUserById(userId));
    }

    @Operation(summary = "新增用户", description = "密码由服务端加密后入库，同时写入用户-角色关联")
    @PreAuthorize("@ss.hasPermi('system:user:add')")
    @PostMapping
    public R<Void> add(@Valid @RequestBody SysUser user) {
        sysUserService.insertUser(user);
        return R.ok();
    }

    @Operation(summary = "修改用户", description = "不处理密码字段：改密码请调用 resetPwd")
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @PutMapping
    public R<Void> edit(@Valid @RequestBody SysUser user) {
        sysUserService.updateUser(user);
        return R.ok();
    }

    @Operation(summary = "删除用户", description = "ids 为逗号分隔字符串；禁止删除超级管理员与当前登录用户")
    @PreAuthorize("@ss.hasPermi('system:user:remove')")
    @DeleteMapping("/{userIds}")
    public R<Void> remove(@PathVariable("userIds") String userIds) {
        sysUserService.deleteUserByIds(StringUtils.splitToLong(userIds));
        return R.ok();
    }

    @Operation(summary = "重置密码", description = "管理员重置他人密码；禁止重置超级管理员密码")
    @PreAuthorize("@ss.hasPermi('system:user:resetPwd')")
    @PutMapping("/resetPwd")
    public R<Void> resetPwd(@Valid @RequestBody ResetPwdBody body) {
        sysUserService.resetPwd(body);
        return R.ok();
    }
}
