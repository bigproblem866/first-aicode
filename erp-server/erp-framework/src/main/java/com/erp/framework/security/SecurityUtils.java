package com.erp.framework.security;

import com.erp.common.core.constant.CommonConstants;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户的取用入口。
 * <p>
 * 数据全部来自 {@code SecurityContextHolder}，不自己维护 ThreadLocal，
 * 因此不会出现「线程池复用导致上一个请求的用户串到下一个请求」这类经典事故。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** 取当前登录用户；未登录返回 null（不抛异常，调用方自己判断） */
    public static LoginUser getLoginUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    /** 取当前登录用户；未登录抛 IllegalStateException（用于「必定已登录」的业务方法） */
    public static LoginUser getLoginUser() {
        LoginUser loginUser = getLoginUserOrNull();
        if (loginUser == null) {
            throw new IllegalStateException("当前无登录用户，请检查该代码是否在请求线程之外被调用");
        }
        return loginUser;
    }

    public static Long getUserId() {
        LoginUser loginUser = getLoginUserOrNull();
        return loginUser == null ? null : loginUser.getUserId();
    }

    public static String getUsername() {
        LoginUser loginUser = getLoginUserOrNull();
        return loginUser == null ? null : loginUser.getUsername();
    }

    public static Long getDeptId() {
        LoginUser loginUser = getLoginUserOrNull();
        return loginUser == null ? null : loginUser.getDeptId();
    }

    /**
     * 取当前用户名，取不到时返回 {@code "system"}。
     * <p>
     * 专供 create_by / update_by 自动填充使用：应用启动时的数据初始化、定时任务等
     * 场景没有登录态，这两个字段也绝不能写 NULL，否则按创建人过滤的查询会漏数据。
     */
    public static String getUsernameOrSystem() {
        String username = getUsername();
        return (username == null || username.isBlank()) ? CommonConstants.SYSTEM_OPERATOR : username;
    }

    /**
     * 是否超级管理员。
     * <p>
     * 按角色标识判断，不看用户 ID —— 主键是数据库自增的，不能假设 admin 一定是 1 号
     * （见 CommonConstants.SUPER_ADMIN_USERNAME 的注释）。
     */
    public static boolean isSuperAdmin() {
        LoginUser loginUser = getLoginUserOrNull();
        return loginUser != null
                && loginUser.getRoles() != null
                && loginUser.getRoles().contains(CommonConstants.SUPER_ADMIN_ROLE_KEY);
    }

    /**
     * 判断当前用户是否拥有某个权限串。超级管理员的 {@code *:*:*} 直接放行。
     * <p>
     * 与 {@link PermissionService#hasPermi(String)} 逻辑一致，
     * 后者给 {@code @PreAuthorize} 用，这个给普通 Java 代码里用。
     */
    public static boolean hasPermission(String permission) {
        LoginUser loginUser = getLoginUserOrNull();
        if (loginUser == null) {
            return false;
        }
        if (loginUser.getPermissions() != null
                && loginUser.getPermissions().contains(CommonConstants.ALL_PERMISSION)) {
            return true;
        }
        return loginUser.getPermissions() != null && loginUser.getPermissions().contains(permission);
    }
}
