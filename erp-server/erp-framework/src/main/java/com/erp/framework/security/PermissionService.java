package com.erp.framework.security;

import com.erp.common.core.constant.CommonConstants;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

/**
 * 权限判断入口，Bean 名固定为 {@code ss}。
 * <p>
 * 用法（配合 SecurityConfig 上的 {@code @EnableMethodSecurity}）：
 * <pre>
 *   &#64;PreAuthorize("@ss.hasPermi('mdm:material:add')")
 *   public R&lt;Void&gt; add(&#64;RequestBody MaterialDTO dto) { ... }
 * </pre>
 * 用户没有权限时抛 AccessDeniedException，由 AccessDeniedHandlerImpl 转成 403 JSON。
 * <p>
 * 权限串约定：{@code 模块:资源:动作}，例如 {@code system:user:list}、{@code inv:stock:query}。
 * 超级管理员持有 {@code *:*:*}，所有判断直接放行。
 * <p>
 * 注意：不要用 Shiro 的 {@code @RequiresPermissions}，本工程没有引 Shiro。
 */
@Component("ss")
public class PermissionService {

    /** 是否拥有指定权限 */
    public boolean hasPermi(String permission) {
        return SecurityUtils.hasPermission(permission);
    }

    /** 是否拥有其中任意一个权限 */
    public boolean hasAnyPermi(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        LoginUser loginUser = SecurityUtils.getLoginUserOrNull();
        if (loginUser == null) {
            return false;
        }
        Set<String> owned = loginUser.getPermissions();
        if (owned == null || owned.isEmpty()) {
            return false;
        }
        if (owned.contains(CommonConstants.ALL_PERMISSION)) {
            return true;
        }
        return Arrays.stream(permissions).anyMatch(owned::contains);
    }

    /** 是否拥有指定角色 */
    public boolean hasRole(String role) {
        LoginUser loginUser = SecurityUtils.getLoginUserOrNull();
        if (loginUser == null || loginUser.getRoles() == null) {
            return false;
        }
        return loginUser.getRoles().contains(role);
    }

    /** 是否拥有其中任意一个角色 */
    public boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }
        return Arrays.stream(roles).anyMatch(this::hasRole);
    }

    /** 是否超级管理员 */
    public boolean isSuperAdmin() {
        LoginUser loginUser = SecurityUtils.getLoginUserOrNull();
        return loginUser != null
                && loginUser.getRoles() != null
                && loginUser.getRoles().contains(CommonConstants.SUPER_ADMIN_ROLE_KEY);
    }
}
