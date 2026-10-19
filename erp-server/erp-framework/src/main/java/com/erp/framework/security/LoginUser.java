package com.erp.framework.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * 登录用户，放在 Spring Security 上下文里（见 {@link SecurityUtils} 取用）。
 * <p>
 * 权限不通过 {@link #getAuthorities()} 暴露：本工程的权限判断统一走
 * {@code @PreAuthorize("@ss.hasPermi('模块:资源:动作')")}，
 * 由 {@link PermissionService} 读 {@link #permissions} 判断。
 * 这样做的好处是权限串格式自由（支持 {@code *:*:*} 通配），不用套 Spring Security 的 ROLE_ 前缀约定。
 */
@Data
public class LoginUser implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long deptId;
    /** 登录名 */
    private String username;
    /** 昵称，界面右上角显示用 */
    private String nickName;

    /** 密码哈希。认证完成后会被清空，绝不外传、绝不入缓存镜像 */
    @JsonIgnore
    private String password;

    /** 权限串集合，如 mdm:material:add；超级管理员为 "*:*:*" */
    private Set<String> permissions = Collections.emptySet();
    /** 角色标识集合，如 admin、warehouse_keeper */
    private Set<String> roles = Collections.emptySet();

    /** Token 唯一标识，也是服务端缓存的 key，登出/踢人时用它删缓存 */
    private String tokenId;
    /** 登录时间（毫秒） */
    private Long loginTime;
    /** 过期时间（毫秒） */
    private Long expireTime;

    /**
     * 不使用 Spring Security 的权限模型，故返回空集合。
     * 真正的权限判断在 {@link PermissionService}。
     */
    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return true;
    }
}
