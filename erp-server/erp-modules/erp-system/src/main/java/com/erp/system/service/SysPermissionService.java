package com.erp.system.service;

import com.erp.common.core.constant.CommonConstants;
import com.erp.system.domain.SysRole;
import com.erp.system.domain.SysUser;
import com.erp.system.mapper.SysMenuMapper;
import com.erp.system.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限装配：把「用户在数据库里的角色与菜单」翻译成 LoginUser 里的 roles / permissions。
 * <p>
 * 单独抽一个类的理由：登录时要构建 LoginUser，将来做权限缓存刷新也要用同一套逻辑，
 * 两处必须完全一致（否则会出现"改了角色但权限没变"这种玄学问题）。
 */
@Service
@RequiredArgsConstructor
public class SysPermissionService {

    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;

    /**
     * 取用户的角色标识集合。
     */
    public Set<String> getRolePermission(SysUser user) {
        if (user == null || user.getId() == null) {
            return Set.of();
        }
        // 兜底：admin 账号恒定拥有超级管理员角色。
        // 万一 sys_user_role 里的关联被误删，也不至于没人能进系统改回来。
        if (CommonConstants.SUPER_ADMIN_USERNAME.equals(user.getUsername())) {
            return Set.of(CommonConstants.SUPER_ADMIN_ROLE_KEY);
        }
        return sysRoleMapper.selectRolesByUserId(user.getId()).stream()
                .map(SysRole::getRoleKey)
                .filter(key -> key != null && !key.isBlank())
                .collect(Collectors.toSet());
    }

    /**
     * 取用户的权限串集合。
     *
     * @param roles  已加载的角色集合（不重复查一遍角色表）
     * @param userId 用户 ID
     * @return 超级管理员返回 {@code *:*:*} 通配符
     */
    public Set<String> getMenuPermission(Set<String> roles, Long userId) {
        // 通配符而不是展开全部权限点：展开的话每次登录要扫全表，
        // 而且新增菜单后旧 Token 里没有新权限，表现为"给管理员加了菜单却点不动"
        if (isSuperAdmin(roles)) {
            return Set.of(CommonConstants.ALL_PERMISSION);
        }
        if (userId == null || roles == null || roles.isEmpty()) {
            return Set.of();
        }
        List<String> perms = sysMenuMapper.selectPermsByUserId(userId);
        return perms.stream()
                .filter(p -> p != null && !p.isBlank())
                .collect(Collectors.toSet());
    }

    /** 是否超级管理员：按角色标识判断 */
    public boolean isSuperAdmin(Set<String> roles) {
        return roles != null && roles.contains(CommonConstants.SUPER_ADMIN_ROLE_KEY);
    }
}
