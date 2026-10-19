package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.core.constant.CommonConstants;
import com.erp.common.core.domain.PageResult;
import com.erp.common.core.exception.ServiceException;
import com.erp.common.core.utils.StringUtils;
import com.erp.framework.security.SecurityUtils;
import com.erp.system.domain.SysUser;
import com.erp.system.domain.dto.ResetPwdBody;
import com.erp.system.domain.dto.SysUserQuery;
import com.erp.system.domain.vo.UserRoleNameVo;
import com.erp.system.mapper.SysRoleMapper;
import com.erp.system.mapper.SysUserMapper;
import com.erp.system.mapper.SysUserRoleMapper;
import com.erp.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户服务实现。
 * <p>
 * 两条贯穿本类的安全规则，改动时务必保持：
 * <ol>
 *   <li>数据库里只存 {@link PasswordEncoder} 加密后的哈希，明文绝不落库；</li>
 *   <li>超级管理员账号（见 {@link CommonConstants#SUPER_ADMIN_USERNAME}）不可被删除，其密码不可被重置，
 *       避免出现"没人能进系统改回来"的锁死局面。</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements ISysUserService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<SysUser> selectUserPage(SysUserQuery query) {
        Page<SysUser> page = query.toPage();
        // 分页参数必须是第一个参数，否则分页插件静默失效（见 SysUserMapper.xml 的说明）
        IPage<SysUser> result = sysUserMapper.selectUserPage(page, query);

        // 列表的「角色」列：roleNames 不是表字段，主查询带不出来。
        // 一次批量查完再按 userId 分组回填，避免逐行查询的 N+1。
        List<SysUser> records = result.getRecords();
        if (!records.isEmpty()) {
            List<Long> userIds = records.stream().map(SysUser::getId).toList();
            Map<Long, List<String>> roleNameMap = sysUserMapper
                    .selectRoleNamesByUserIds(userIds)
                    .stream()
                    .collect(Collectors.groupingBy(
                            UserRoleNameVo::getUserId,
                            Collectors.mapping(UserRoleNameVo::getRoleName, Collectors.toList())));
            records.forEach(user -> user.setRoleNames(
                    roleNameMap.getOrDefault(user.getId(), List.of())));
        }

        return PageResult.of(result);
    }

    @Override
    public SysUser selectUserById(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        // 编辑页要回显角色勾选，用两个专用查询带出 roleIds 与 roleNames
        user.setRoleIds(sysRoleMapper.selectRoleIdsByUserId(userId));
        user.setRoleNames(sysUserMapper.selectRoleNamesByUserId(userId));
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertUser(SysUser user) {
        // 先查一次给准确提示；并发下仍靠 uk_sys_user_username 唯一索引兜底
        checkUsernameUnique(user.getUsername(), null);

        if (StringUtils.isBlank(user.getPassword())) {
            throw new ServiceException("密码不能为空");
        }
        // 明文密码到此为止，后续一律用哈希
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (StringUtils.isBlank(user.getStatus())) {
            user.setStatus(CommonConstants.STATUS_NORMAL);
        }

        sysUserMapper.insert(user);
        insertUserRole(user.getId(), user.getRoleIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(SysUser user) {
        if (user.getId() == null) {
            throw new ServiceException("用户ID不能为空");
        }
        SysUser existing = sysUserMapper.selectById(user.getId());
        if (existing == null) {
            throw new ServiceException("用户不存在");
        }

        // 不允许通过修改接口改密码：前端传什么密码都丢弃，改密码只能走 resetPwd
        user.setPassword(null);

        // 用户名只能保持原样或改成没人用过的；不允许占用别人的账号名
        if (StringUtils.isNotBlank(user.getUsername())
                && !user.getUsername().equals(existing.getUsername())) {
            checkUsernameUnique(user.getUsername(), user.getId());
        }

        sysUserMapper.updateById(user);

        // 角色关联同步：先清空再逐条插入，前端每次传的是完整勾选结果。
        //
        // roleIds 为 null 与为空列表是两种不同语义，必须区分开：
        //   null      → 本次请求不涉及角色（比如只改昵称的接口调用），保持原有关联不动
        //   空列表 [] → 明确要清空该用户的所有角色
        // 如果不区分，任何漏传 roleIds 的调用都会静默把用户的角色清空，
        // 症状是"用户突然什么菜单都看不见了"，而接口返回还是成功的 —— 很难查。
        if (user.getRoleIds() != null) {
            sysUserRoleMapper.deleteByUserId(user.getId());
            insertUserRole(user.getId(), user.getRoleIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserByIds(List<Long> userIds) {
        if (StringUtils.isEmpty(userIds)) {
            throw new ServiceException("请选择要删除的用户");
        }
        Long currentUserId = SecurityUtils.getUserId();
        for (Long userId : userIds) {
            SysUser user = sysUserMapper.selectById(userId);
            if (user == null) {
                continue;
            }
            if (CommonConstants.SUPER_ADMIN_USERNAME.equals(user.getUsername())) {
                throw new ServiceException("不允许删除超级管理员账号");
            }
            if (userId.equals(currentUserId)) {
                throw new ServiceException("不允许删除当前登录用户");
            }
        }

        // 逻辑删除：只把 is_deleted 置 1，数据仍在库里可追溯
        sysUserMapper.deleteByIds(userIds);
        // 顺带清掉角色关联。这张关联表没有逻辑删除字段，用户没了则关联也无意义，
        // 留着只会让"某角色下有多少用户"的统计多算。
        for (Long userId : userIds) {
            sysUserRoleMapper.deleteByUserId(userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPwd(ResetPwdBody body) {
        SysUser user = sysUserMapper.selectById(body.getUserId());
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        if (CommonConstants.SUPER_ADMIN_USERNAME.equals(user.getUsername())) {
            throw new ServiceException("不允许重置超级管理员密码");
        }

        // 只更新密码一列：MyBatis-Plus 的默认更新策略会忽略 null 字段
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(body.getPassword()));
        sysUserMapper.updateById(update);
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    /** 登录账号查重。excludeId 用于修改时排除自己 */
    private void checkUsernameUnique(String username, Long excludeId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username);
        if (excludeId != null) {
            wrapper.ne(SysUser::getId, excludeId);
        }
        if (sysUserMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("登录账号已存在：" + username);
        }
    }

    /** 写入用户-角色关联，空集合安全 */
    private void insertUserRole(Long userId, List<Long> roleIds) {
        if (StringUtils.isEmpty(roleIds)) {
            return;
        }
        for (Long roleId : roleIds) {
            if (roleId != null) {
                sysUserRoleMapper.insert(userId, roleId);
            }
        }
    }
}
