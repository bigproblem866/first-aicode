package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.core.constant.CommonConstants;
import com.erp.common.core.domain.PageResult;
import com.erp.common.core.exception.ServiceException;
import com.erp.common.core.utils.StringUtils;
import com.erp.system.domain.SysRole;
import com.erp.system.domain.dto.SysRoleQuery;
import com.erp.system.mapper.SysRoleMapper;
import com.erp.system.mapper.SysRoleMenuMapper;
import com.erp.system.mapper.SysUserRoleMapper;
import com.erp.system.service.ISysMenuService;
import com.erp.system.service.ISysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色服务实现。
 * <p>
 * 两条不可越过的底线：
 * <ul>
 *   <li>超级管理员角色（roleKey = {@code admin}）的 roleKey 不可改、角色本身不可删，
 *       否则权限判断的兜底链路会被切断；</li>
 *   <li>角色还挂在用户身上时不可删，避免 sys_user_role 里留下指向不存在角色的关联。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements ISysRoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final ISysMenuService sysMenuService;

    @Override
    public PageResult<SysRole> selectRolePage(SysRoleQuery query) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            wrapper.like(StringUtils.isNotBlank(query.getRoleName()), SysRole::getRoleName, query.getRoleName());
            wrapper.like(StringUtils.isNotBlank(query.getRoleKey()), SysRole::getRoleKey, query.getRoleKey());
            wrapper.eq(StringUtils.isNotBlank(query.getStatus()), SysRole::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(SysRole::getRoleSort);
        return PageResult.of(sysRoleMapper.selectPage(query.toPage(), wrapper));
    }

    @Override
    public SysRole selectRoleById(Long roleId) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new ServiceException("角色不存在");
        }
        role.setMenuIds(sysMenuService.selectMenuIdsByRoleId(roleId));
        return role;
    }

    @Override
    public List<SysRole> selectRoleAll() {
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, CommonConstants.STATUS_NORMAL)
                .orderByAsc(SysRole::getRoleSort));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertRole(SysRole role) {
        checkRoleKeyUnique(role.getRoleKey(), null);
        if (StringUtils.isBlank(role.getStatus())) {
            role.setStatus(CommonConstants.STATUS_NORMAL);
        }
        sysRoleMapper.insert(role);
        insertRoleMenu(role.getId(), role.getMenuIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(SysRole role) {
        if (role.getId() == null) {
            throw new ServiceException("角色ID不能为空");
        }
        SysRole existing = sysRoleMapper.selectById(role.getId());
        if (existing == null) {
            throw new ServiceException("角色不存在");
        }
        // 超级管理员角色的 roleKey 是权限判断的锚点，一旦被改掉，兜底逻辑就失效了
        if (StringUtils.isNotBlank(role.getRoleKey())
                && CommonConstants.SUPER_ADMIN_ROLE_KEY.equals(existing.getRoleKey())
                && !existing.getRoleKey().equals(role.getRoleKey())) {
            throw new ServiceException("不允许修改超级管理员角色的权限标识");
        }
        checkRoleKeyUnique(role.getRoleKey(), role.getId());

        sysRoleMapper.updateById(role);
        // 菜单关联同步：先清空再逐条插入，前端每次传的是完整勾选结果
        sysRoleMenuMapper.deleteByRoleId(role.getId());
        insertRoleMenu(role.getId(), role.getMenuIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoleByIds(List<Long> roleIds) {
        if (StringUtils.isEmpty(roleIds)) {
            throw new ServiceException("请选择要删除的角色");
        }
        for (Long roleId : roleIds) {
            SysRole role = sysRoleMapper.selectById(roleId);
            if (role == null) {
                continue;
            }
            if (CommonConstants.SUPER_ADMIN_ROLE_KEY.equals(role.getRoleKey())) {
                throw new ServiceException("不允许删除超级管理员角色");
            }
            int assignedCount = sysUserRoleMapper.countByRoleId(roleId);
            if (assignedCount > 0) {
                throw new ServiceException("角色【" + role.getRoleName() + "】已分配给 "
                        + assignedCount + " 个用户，请先解除分配后再删除");
            }
        }

        sysRoleMapper.deleteByIds(roleIds);
        // 清理角色-菜单关联，避免菜单授权表里留下指向已删角色的记录
        for (Long roleId : roleIds) {
            sysRoleMenuMapper.deleteByRoleId(roleId);
        }
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    /** 角色标识查重。excludeId 用于修改时排除自己 */
    private void checkRoleKeyUnique(String roleKey, Long excludeId) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, roleKey);
        if (excludeId != null) {
            wrapper.ne(SysRole::getId, excludeId);
        }
        if (sysRoleMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("角色标识已存在：" + roleKey);
        }
    }

    /** 写入角色-菜单关联，空集合安全 */
    private void insertRoleMenu(Long roleId, List<Long> menuIds) {
        if (StringUtils.isEmpty(menuIds)) {
            return;
        }
        for (Long menuId : menuIds) {
            if (menuId != null) {
                sysRoleMenuMapper.insert(roleId, menuId);
            }
        }
    }
}
