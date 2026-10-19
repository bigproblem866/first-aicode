package com.erp.system.service;

import com.erp.common.core.domain.PageResult;
import com.erp.system.domain.SysRole;
import com.erp.system.domain.dto.SysRoleQuery;

import java.util.List;

/**
 * 角色服务。
 * <p>
 * 角色是"用户"与"菜单权限"之间的桥梁：用户挂角色，角色挂菜单，登录时再把这条链翻译成权限串。
 * 因此本类里所有涉及角色增删改的方法都必须同步维护 sys_role_menu。
 */
public interface ISysRoleService {

    /** 角色分页列表 */
    PageResult<SysRole> selectRolePage(SysRoleQuery query);

    /** 角色详情，附带该角色已授权的菜单 ID（编辑页回显勾选） */
    SysRole selectRoleById(Long roleId);

    /** 全部正常状态角色（不分页），给用户分配角色时的下拉用 */
    List<SysRole> selectRoleAll();

    /** 新增角色：roleKey 查重 + 写入菜单关联 */
    void insertRole(SysRole role);

    /** 修改角色：禁止改超级管理员角色的 roleKey，同步菜单关联 */
    void updateRole(SysRole role);

    /** 批量逻辑删除角色。禁止删超级管理员角色；已分配给用户的角色须先解除分配 */
    void deleteRoleByIds(List<Long> roleIds);
}
