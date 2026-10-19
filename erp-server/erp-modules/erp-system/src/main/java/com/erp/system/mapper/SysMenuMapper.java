package com.erp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.system.domain.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜单 Mapper。
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /** 查询用户拥有的全部权限串（去重），登录时构建 LoginUser 用 */
    List<String> selectPermsByUserId(@Param("userId") Long userId);

    /** 查询用户可见的菜单（只含 M 目录与 C 菜单，不含 F 按钮），构建前端路由用 */
    List<SysMenu> selectMenuTreeByUserId(@Param("userId") Long userId);

    /** 查询全部菜单，超级管理员用 */
    List<SysMenu> selectMenuTreeAll();

    /** 查询角色已授权的菜单 ID，角色编辑页回显勾选状态用 */
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);
}
