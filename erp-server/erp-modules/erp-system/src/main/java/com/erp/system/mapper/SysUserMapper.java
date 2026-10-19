package com.erp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.system.domain.SysUser;
import com.erp.system.domain.dto.SysUserQuery;
import com.erp.system.domain.vo.UserRoleNameVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户 Mapper。
 * <p>
 * 单表增删改查一律用 BaseMapper 自带方法（selectById / insert / updateById ...），
 * 只有联表或多表拼装才写 XML。这条规则能省掉大量无意义的 XML 维护成本。
 * <p>
 * ⚠ 注意：BaseMapper 的方法会自动带上逻辑删除条件（is_deleted = 0），
 * 但<b>自己写的 XML 不会</b>，必须手写 {@code and u.is_deleted = 0}。
 * 这是 MyBatis-Plus 项目里最容易漏、且漏了以后很久才发现的一处。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 用户分页（联表带出部门名称） */
    IPage<SysUser> selectUserPage(IPage<SysUser> page, @Param("q") SysUserQuery query);

    /** 按登录名查用户，登录认证用 */
    SysUser selectUserByUsername(@Param("username") String username);

    /** 查询用户拥有的角色标识，如 ["admin"] */
    List<String> selectRoleKeysByUserId(@Param("userId") Long userId);

    /** 查询用户拥有的角色名称，列表页展示用 */
    List<String> selectRoleNamesByUserId(@Param("userId") Long userId);

    /** 批量查询多个用户的角色名称（列表页一次查完，避免 N+1） */
    List<UserRoleNameVo> selectRoleNamesByUserIds(@Param("userIds") List<Long> userIds);
}
