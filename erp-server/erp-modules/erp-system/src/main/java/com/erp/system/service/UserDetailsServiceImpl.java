package com.erp.system.service;

import com.erp.framework.security.LoginUser;
import com.erp.system.domain.SysUser;
import com.erp.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Spring Security 的用户加载实现，登录时由 {@code AuthenticationManager} 调用。
 * <p>
 * <b>为什么这里不抛"账号已停用"</b>：DaoAuthenticationProvider 会把
 * {@code loadUserByUsername} 里抛出的非 UsernameNotFoundException 包装成
 * AuthenticationServiceException，原始提示会丢失，前端只能看到"认证服务异常"。
 * 所以停用校验放在认证成功之后的 {@link SysLoginService} 里做，提示信息可控。
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final SysPermissionService sysPermissionService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectUserByUsername(username);
        if (user == null) {
            // 这里抛 UsernameNotFound 会被 Spring Security 统一转成 BadCredentials，
            // 前端一律收到"用户名或密码错误" —— 刻意不区分，避免被用来探测哪些账号存在
            throw new UsernameNotFoundException("用户名或密码错误");
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setUsername(user.getUsername());
        loginUser.setNickName(user.getNickName());
        // 密码哈希留着，交给 DaoAuthenticationProvider 做 matches 校验；
        // 登录成功后由 SysLoginService 清空，不允许进入缓存
        loginUser.setPassword(user.getPassword());

        // 先算角色，再把角色传给权限计算 —— 避免在权限计算里把角色表再查一遍，
        // 也让「超级管理员直接拿 *:*:*」这条规则只在一处生效
        Set<String> roles = sysPermissionService.getRolePermission(user);
        loginUser.setRoles(roles);
        loginUser.setPermissions(sysPermissionService.getMenuPermission(roles, user.getId()));
        return loginUser;
    }
}
