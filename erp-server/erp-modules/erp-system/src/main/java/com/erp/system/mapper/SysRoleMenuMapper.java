package com.erp.system.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色-菜单关联 Mapper。同样是纯关联表，用注解写。
 */
@Mapper
public interface SysRoleMenuMapper {

    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_role_menu WHERE menu_id = #{menuId}")
    int deleteByMenuId(@Param("menuId") Long menuId);

    /** 幂等插入，重复授权不报错 */
    @Insert("INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (#{roleId}, #{menuId})")
    int insert(@Param("roleId") Long roleId, @Param("menuId") Long menuId);

    /**
     * 统计有多少个角色引用了该菜单。
     * <p>
     * 删菜单前用它给出更准确的提示："该菜单已被 3 个角色引用"，
     * 比笼统的"删除失败"有用得多。
     */
    @org.apache.ibatis.annotations.Select("SELECT COUNT(1) FROM sys_role_menu WHERE menu_id = #{menuId}")
    int countByMenuId(@Param("menuId") Long menuId);
}
