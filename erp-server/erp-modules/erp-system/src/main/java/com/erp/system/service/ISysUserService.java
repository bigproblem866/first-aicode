package com.erp.system.service;

import com.erp.common.core.domain.PageResult;
import com.erp.system.domain.SysUser;
import com.erp.system.domain.dto.ResetPwdBody;
import com.erp.system.domain.dto.SysUserQuery;

import java.util.List;

/**
 * 用户服务。
 * <p>
 * 覆盖用户管理页的全部动作：分页查询、详情（含角色）、新增、修改、删除、重置密码。
 * 密码相关的两条规则在所有入口都要守住：<b>入库存哈希</b>、<b>不允许改超级管理员密码</b>。
 */
public interface ISysUserService {

    /** 用户分页列表（联表带出部门名称） */
    PageResult<SysUser> selectUserPage(SysUserQuery query);

    /** 用户详情，附带其角色 ID 与角色名称，供编辑页回显勾选 */
    SysUser selectUserById(Long userId);

    /** 新增用户：用户名查重 + 密码加密 + 写入角色关联 */
    void insertUser(SysUser user);

    /** 修改用户：不允许改密码（密码走 {@link #resetPwd}），同步角色关联 */
    void updateUser(SysUser user);

    /** 批量逻辑删除用户。禁止删超级管理员、禁止删自己 */
    void deleteUserByIds(List<Long> userIds);

    /** 重置他人密码。禁止重置超级管理员密码 */
    void resetPwd(ResetPwdBody body);
}
